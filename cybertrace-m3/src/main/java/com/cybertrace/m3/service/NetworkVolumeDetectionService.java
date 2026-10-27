package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to detect anomalous network traffic volume from normalized forensic events.
 * Uses a robust data-driven statistical baseline based on Median and Median Absolute
 * Deviation (MAD) to identify exfiltration or data transfer spikes without relying on
 * arbitrary hard-coded byte limits.
 */
@Service
public class NetworkVolumeDetectionService {

    public static final double DEFAULT_MODIFIED_Z_THRESHOLD = 3.5;
    public static final int DEFAULT_MIN_SAMPLE_SIZE = 3;
    public static final String DETECTION_METHOD = "STATISTICAL_MEDIAN_ABSOLUTE_DEVIATION";

    private final double modifiedZThreshold;
    private final int minSampleSize;

    public NetworkVolumeDetectionService(
            @Value("${cybertrace.m3.network-volume.threshold-z:3.5}") double modifiedZThreshold,
            @Value("${cybertrace.m3.network-volume.min-sample-size:3}") int minSampleSize
    ) {
        this.modifiedZThreshold = modifiedZThreshold > 0.0 ? modifiedZThreshold : DEFAULT_MODIFIED_Z_THRESHOLD;
        this.minSampleSize = minSampleSize >= 2 ? minSampleSize : DEFAULT_MIN_SAMPLE_SIZE;
    }

    public NetworkVolumeDetectionService(double modifiedZThreshold) {
        this(modifiedZThreshold, DEFAULT_MIN_SAMPLE_SIZE);
    }

    public NetworkVolumeDetectionService() {
        this(DEFAULT_MODIFIED_Z_THRESHOLD, DEFAULT_MIN_SAMPLE_SIZE);
    }

    public double getModifiedZThreshold() {
        return modifiedZThreshold;
    }

    public int getMinSampleSize() {
        return minSampleSize;
    }

    /**
     * Analyzes normalized events and flags unusually high network volume transfers.
     *
     * @param events list of normalized events from M2
     * @return list of detected network volume anomaly findings
     */
    public List<AnomalyFindingDTO> detect(List<NormalizedEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Filter valid NETWORK_EVENT items with non-null, non-negative bytes
        List<NormalizedEventDTO> networkEvents = events.stream()
                .filter(Objects::nonNull)
                .filter(e -> "NETWORK_EVENT".equalsIgnoreCase(e.getEventType()))
                .filter(e -> e.getBytes() != null && e.getBytes() >= 0)
                .collect(Collectors.toList());

        // 2. Ensure minimum sample size for a meaningful statistical baseline
        if (networkEvents.size() < minSampleSize) {
            return Collections.emptyList();
        }

        // 3. Extract and sort byte values
        List<Double> byteValues = networkEvents.stream()
                .map(e -> e.getBytes().doubleValue())
                .sorted()
                .collect(Collectors.toList());

        // 4. Calculate Median (robust central tendency)
        double median = calculateMedian(byteValues);

        // 5. Calculate Absolute Deviations from Median and MAD
        List<Double> absoluteDeviations = byteValues.stream()
                .map(b -> Math.abs(b - median))
                .sorted()
                .collect(Collectors.toList());

        double mad = calculateMedian(absoluteDeviations);

        // 6. Scale estimation (standard normal consistency multiplier 1.4826)
        double scale;
        if (mad > 0.0) {
            scale = 1.4826 * mad;
        } else {
            // Fallback to Mean Absolute Deviation to handle distributions where >50% values are identical
            double sumDev = 0.0;
            for (Double dev : absoluteDeviations) {
                sumDev += dev;
            }
            double meanDeviation = sumDev / absoluteDeviations.size();
            scale = 1.2533 * meanDeviation;
        }

        // If scale is 0.0, all values in the batch are identical; no outliers possible
        if (scale <= 0.0) {
            return Collections.emptyList();
        }

        // 7. Evaluate each event against statistical deviation threshold
        List<AnomalyFindingDTO> findings = new ArrayList<>();
        for (NormalizedEventDTO event : networkEvents) {
            double bytes = event.getBytes().doubleValue();

            // We only flag unusually high transfers (exfiltration spikes)
            if (bytes > median) {
                double modifiedZ = (bytes - median) / scale;

                if (modifiedZ >= modifiedZThreshold) {
                    AnomalySeverity severity = calculateSeverity(modifiedZ);
                    double score = calculateScore(modifiedZ);
                    String reason = buildReason(event, bytes, median, modifiedZ);

                    AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                            .eventId(event.getEventId())
                            .timestamp(event.getTimestamp())
                            .eventType("NETWORK_EVENT")
                            .anomalyType(AnomalyType.NETWORK_VOLUME)
                            .reason(reason)
                            .severity(severity)
                            .score(score)
                            .detectionMethod(DETECTION_METHOD)
                            .build();

                    findings.add(finding);
                }
            }
        }

        return findings;
    }

    private static double calculateMedian(List<Double> sortedValues) {
        int size = sortedValues.size();
        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        } else {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        }
    }

    private AnomalySeverity calculateSeverity(double modifiedZ) {
        if (modifiedZ >= modifiedZThreshold * 3.0) {
            return AnomalySeverity.CRITICAL;
        } else if (modifiedZ >= modifiedZThreshold * 2.0) {
            return AnomalySeverity.HIGH;
        } else {
            return AnomalySeverity.MEDIUM;
        }
    }

    private double calculateScore(double modifiedZ) {
        // Base score 0.70 at threshold, scaling smoothly up to 0.99
        double rawScore = 0.70 + Math.min(0.29, (modifiedZ - modifiedZThreshold) * 0.03);
        double boundedScore = Math.max(0.0, Math.min(0.99, rawScore));
        return Math.round(boundedScore * 100.0) / 100.0;
    }

    private String buildReason(NormalizedEventDTO event, double bytes, double median, double modifiedZ) {
        String source = event.getIpAddress() != null && !event.getIpAddress().isBlank()
                ? event.getIpAddress()
                : "<UNKNOWN_SRC>";

        String destination = (event.getDestinationIp() != null && !event.getDestinationIp().isBlank()
                ? event.getDestinationIp()
                : "<UNKNOWN_DEST>") +
                (event.getPort() != null ? ":" + event.getPort() : "");

        return String.format(
                "Network transfer of %.0f bytes from IP '%s' to '%s' is unusually high compared with the investigation's normal baseline (median: %.0f bytes, modified Z-score: %.2f, threshold: %.1f)",
                bytes, source, destination, median, modifiedZ, modifiedZThreshold
        );
    }
}
