package com.cybertrace.m3.service;

import com.cybertrace.m3.model.AnomalyFindingDTO;
import com.cybertrace.m3.model.AnomalySeverity;
import com.cybertrace.m3.model.AnomalyType;
import com.cybertrace.m3.model.NormalizedEventDTO;
import com.cybertrace.m3.util.EventStatusExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import smile.anomaly.IsolationForest;
import smile.anomaly.IsolationTree;
import smile.math.MathEx;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Machine Learning Anomaly Detection Service utilizing Smile's Isolation Forest algorithm.
 *
 * Architecture separates:
 * 1. Model Training / Fitting: Performed strictly on historical/legitimate baseline events.
 * 2. Anomaly Scoring: Performed strictly on incoming case investigation events evaluated
 *    against the pre-fitted model.
 *
 * Case events are NEVER used to train the model, and are NEVER appended to the baseline.
 */
@Service
public class MachineLearningAnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(MachineLearningAnomalyDetectionService.class);
    private static final String DETECTION_METHOD = "Smile 3.1.0 Isolation Forest";

    private final double scoreThreshold;
    private final int numTrees;
    private final double subsample;
    private final long randomSeed;
    private final int minBatchSize;
    private HistoricalBaselineProvider baselineProvider;

    /**
     * Immutable snapshot of a trained Isolation Forest model along with
     * the baseline feature statistics and active column mapping.
     */
    public record TrainedModel(
            IsolationForest forest,
            double[] baselineMeans,
            double[] baselineStdDevs,
            List<Integer> activeCols
    ) {}

    private volatile TrainedModel trainedModel;

    public MachineLearningAnomalyDetectionService() {
        this(new InMemoryHistoricalBaselineProvider(), 0.60, 100, 0.7, 42L, 5);
    }

    public MachineLearningAnomalyDetectionService(
            @Value("${cybertrace.m3.machine-learning.score-threshold:0.60}") double scoreThreshold,
            @Value("${cybertrace.m3.machine-learning.num-trees:100}") int numTrees,
            @Value("${cybertrace.m3.machine-learning.subsample:0.7}") double subsample,
            @Value("${cybertrace.m3.machine-learning.random-seed:42}") long randomSeed,
            @Value("${cybertrace.m3.machine-learning.min-batch-size:5}") int minBatchSize
    ) {
        this(new InMemoryHistoricalBaselineProvider(), scoreThreshold, numTrees, subsample, randomSeed, minBatchSize);
    }

    @Autowired
    public MachineLearningAnomalyDetectionService(
            HistoricalBaselineProvider baselineProvider,
            @Value("${cybertrace.m3.machine-learning.score-threshold:0.60}") double scoreThreshold,
            @Value("${cybertrace.m3.machine-learning.num-trees:100}") int numTrees,
            @Value("${cybertrace.m3.machine-learning.subsample:0.7}") double subsample,
            @Value("${cybertrace.m3.machine-learning.random-seed:42}") long randomSeed,
            @Value("${cybertrace.m3.machine-learning.min-batch-size:5}") int minBatchSize
    ) {
        this.baselineProvider = baselineProvider != null ? baselineProvider : new InMemoryHistoricalBaselineProvider();
        this.scoreThreshold = scoreThreshold;
        this.numTrees = numTrees;
        this.subsample = subsample;
        this.randomSeed = randomSeed;
        this.minBatchSize = minBatchSize;
    }

    public double getScoreThreshold() {
        return scoreThreshold;
    }

    public int getNumTrees() {
        return numTrees;
    }

    public double getSubsample() {
        return subsample;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public int getMinBatchSize() {
        return minBatchSize;
    }

    public HistoricalBaselineProvider getBaselineProvider() {
        return baselineProvider;
    }

    public void setBaselineProvider(HistoricalBaselineProvider baselineProvider) {
        this.baselineProvider = baselineProvider != null ? baselineProvider : new InMemoryHistoricalBaselineProvider();
    }

    /**
     * Checks whether an Isolation Forest model has been fitted and is ready for scoring.
     *
     * @return true if fitted, false otherwise
     */
    public boolean isFitted() {
        return trainedModel != null;
    }

    /**
     * Clears the current trained model state.
     */
    public void resetModel() {
        this.trainedModel = null;
    }

    /**
     * Convenience method to register legitimate baseline events and immediately fit the model.
     *
     * @param baselineEvents legitimate historical baseline events
     * @return true if fitting succeeded, false otherwise
     */
    public boolean setBaseline(List<NormalizedEventDTO> baselineEvents) {
        if (this.baselineProvider instanceof InMemoryHistoricalBaselineProvider inMemory) {
            inMemory.setBaselineEvents(baselineEvents);
        }
        return fit(baselineEvents);
    }

    /**
     * Fits the Isolation Forest model strictly using the provided legitimate/historical baseline events.
     * Incoming case investigation events MUST NOT be passed to this method.
     *
     * @param baselineEvents list of legitimate historical evidence events (unmutated)
     * @return true if training succeeded and model was fitted, false if input was insufficient
     */
    public synchronized boolean fit(List<NormalizedEventDTO> baselineEvents) {
        if (baselineEvents == null || baselineEvents.isEmpty()) {
            log.debug("Baseline events are null or empty; cannot fit Isolation Forest");
            this.trainedModel = null;
            return false;
        }

        List<NormalizedEventDTO> validBaseline = baselineEvents.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validBaseline.size() < minBatchSize) {
            log.debug("Baseline size {} is below minimum batch size {}; skipping model training",
                    validBaseline.size(), minBatchSize);
            this.trainedModel = null;
            return false;
        }

        int n = validBaseline.size();
        final int featureCount = 6;
        double[][] featureMatrix = new double[n][featureCount];

        for (int i = 0; i < n; i++) {
            featureMatrix[i] = extractFeatures(validBaseline.get(i));
        }

        // Compute baseline column statistics for normalization and explainability
        double[] colMeans = new double[featureCount];
        double[] colStdDevs = new double[featureCount];

        for (int j = 0; j < featureCount; j++) {
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                sum += featureMatrix[i][j];
            }
            colMeans[j] = sum / n;

            double sumSqDiff = 0.0;
            for (int i = 0; i < n; i++) {
                double diff = featureMatrix[i][j] - colMeans[j];
                sumSqDiff += diff * diff;
            }
            colStdDevs[j] = Math.sqrt(sumSqDiff / (n > 1 ? (n - 1) : 1));
        }

        // Identify active features with non-zero variance in baseline
        List<Integer> activeCols = new ArrayList<>();
        for (int j = 0; j < featureCount; j++) {
            if (colStdDevs[j] > 1e-6) {
                activeCols.add(j);
            }
        }

        if (activeCols.isEmpty()) {
            log.debug("All baseline events have zero variance; cannot fit Isolation Forest");
            this.trainedModel = null;
            return false;
        }

        int activeDim = activeCols.size();
        double[][] activeMatrix = new double[n][activeDim];
        for (int i = 0; i < n; i++) {
            for (int d = 0; d < activeDim; d++) {
                activeMatrix[i][d] = featureMatrix[i][activeCols.get(d)];
            }
        }

        // Train Isolation Forest deterministically on baseline
        int maxDepth = (int) Math.max(1, Math.ceil(MathEx.log2(n)));
        synchronized (MachineLearningAnomalyDetectionService.class) {
            MathEx.setSeed(randomSeed);
            int k = (int) Math.round(n * subsample);
            if (k < 1) k = 1;
            if (k > n) k = n;

            IsolationTree[] trees = new IsolationTree[numTrees];
            for (int t = 0; t < numTrees; t++) {
                int[] perm = MathEx.permutate(n);
                List<double[]> sample = new ArrayList<>(k);
                for (int s = 0; s < k; s++) {
                    sample.add(activeMatrix[perm[s]]);
                }
                trees[t] = new IsolationTree(sample, maxDepth, 0);
            }
            IsolationForest forest = new IsolationForest(n, 0, trees);
            this.trainedModel = new TrainedModel(forest, colMeans, colStdDevs, activeCols);
        }

        log.debug("Fitted Isolation Forest on {} baseline events (features={}, trees={}, seed={})",
                n, activeDim, numTrees, randomSeed);
        return true;
    }

    /**
     * Scores incoming case events against the fitted baseline Isolation Forest model.
     * Case events are strictly evaluated against the baseline model; they are NEVER
     * used to train the model and NEVER appended to the baseline.
     *
     * @param caseEvents input list of case events to evaluate (unmutated)
     * @return list of identified {@link AnomalyFindingDTO} objects
     */
    public List<AnomalyFindingDTO> score(List<NormalizedEventDTO> caseEvents) {
        if (caseEvents == null || caseEvents.isEmpty()) {
            return Collections.emptyList();
        }

        TrainedModel model = this.trainedModel;
        if (model == null) {
            // Lazy-fit from baseline provider if available
            if (baselineProvider != null) {
                List<NormalizedEventDTO> providerEvents = baselineProvider.getBaselineEvents();
                if (providerEvents != null && !providerEvents.isEmpty()) {
                    fit(providerEvents);
                    model = this.trainedModel;
                }
            }
        }

        if (model == null) {
            log.debug("Isolation Forest has no fitted baseline model; returning zero ML findings");
            return Collections.emptyList();
        }

        List<NormalizedEventDTO> validEvents = caseEvents.stream()
                .filter(Objects::nonNull)
                .toList();

        if (validEvents.isEmpty()) {
            return Collections.emptyList();
        }

        int m = validEvents.size();
        List<Integer> activeCols = model.activeCols();
        int activeDim = activeCols.size();

        double[][] caseFeatureMatrix = new double[m][6];
        double[][] caseActiveMatrix = new double[m][activeDim];

        for (int i = 0; i < m; i++) {
            caseFeatureMatrix[i] = extractFeatures(validEvents.get(i));
            for (int d = 0; d < activeDim; d++) {
                caseActiveMatrix[i][d] = caseFeatureMatrix[i][activeCols.get(d)];
            }
        }

        // Score case events strictly against the pre-fitted baseline forest
        double[] anomalyScores = model.forest().score(caseActiveMatrix);

        List<AnomalyFindingDTO> findings = new ArrayList<>();
        Set<String> seenEventIds = new HashSet<>();

        for (int i = 0; i < m; i++) {
            double rawScore = anomalyScores[i];
            log.debug("Case event [{}] anomaly score={}", validEvents.get(i).getEventId(), rawScore);

            if (rawScore >= scoreThreshold) {
                NormalizedEventDTO event = validEvents.get(i);
                String eventId = event.getEventId();

                if (eventId != null && !seenEventIds.add(eventId)) {
                    continue;
                }

                double normalizedScore = roundScore(rawScore);
                AnomalySeverity severity = determineSeverity(rawScore);
                String reason = buildExplainableReason(
                        event,
                        caseFeatureMatrix[i],
                        model.baselineMeans(),
                        model.baselineStdDevs(),
                        rawScore
                );

                AnomalyFindingDTO finding = AnomalyFindingDTO.builder()
                        .eventId(eventId)
                        .timestamp(event.getTimestamp())
                        .eventType(event.getEventType())
                        .anomalyType(AnomalyType.MACHINE_LEARNING)
                        .reason(reason)
                        .severity(severity)
                        .score(normalizedScore)
                        .detectionMethod(String.format("%s (trees=%d, seed=%d)", DETECTION_METHOD, numTrees, randomSeed))
                        .build();

                findings.add(finding);
            }
        }

        return findings;
    }

    /**
     * Primary detection entry point called by {@link AnalysisService}.
     * Evaluates incoming case events against the fitted baseline model without retraining.
     *
     * @param events incoming case investigation events (unmutated)
     * @return list of identified findings
     */
    public List<AnomalyFindingDTO> detect(List<NormalizedEventDTO> events) {
        return score(events);
    }

    /**
     * Fits the model on the supplied baseline events, then scores the incoming case events.
     * Case events remain completely separated from baseline events.
     *
     * @param caseEvents incoming case investigation events
     * @param baselineEvents legitimate historical baseline events
     * @return list of findings detected in caseEvents
     */
    public List<AnomalyFindingDTO> detect(List<NormalizedEventDTO> caseEvents, List<NormalizedEventDTO> baselineEvents) {
        fit(baselineEvents);
        return score(caseEvents);
    }

    /**
     * Extracts a 6-dimensional deterministic numerical feature vector from a normalized event:
     * [0] Hour of Day (0.0 - 23.0)
     * [1] Event Type Encoded (1.0 - 5.0)
     * [2] Log-Scaled Bytes: log1p(bytes)
     * [3] Normalized Port (0.0 - 1.0)
     * [4] Operation/Login Failure (1.0 if failed, 0.0 otherwise)
     * [5] Action Encoded (1.0 - 7.0)
     */
    private double[] extractFeatures(NormalizedEventDTO event) {
        double[] f = new double[6];

        // 0. Hour of day (default to 12.0 if timestamp is null)
        f[0] = (event.getTimestamp() != null) ? (double) event.getTimestamp().getHour() : 12.0;

        // 1. Event Type Encoded
        if (event.getEventType() != null) {
            f[1] = switch (event.getEventType().trim().toUpperCase()) {
                case "LOGIN" -> 1.0;
                case "FILE_ACCESS" -> 2.0;
                case "NETWORK_EVENT" -> 3.0;
                case "BROWSER_ACTIVITY" -> 4.0;
                default -> 5.0;
            };
        } else {
            f[1] = 0.0;
        }

        // 2. Log-scaled bytes: log1p(bytes)
        if (event.getBytes() != null && event.getBytes() > 0) {
            f[2] = Math.log1p(event.getBytes().doubleValue());
        } else {
            f[2] = 0.0;
        }

        // 3. Normalized port [0.0, 1.0]
        if (event.getPort() != null && event.getPort() >= 0) {
            f[3] = (double) Math.min(event.getPort(), 65535) / 65535.0;
        } else {
            f[3] = 0.0;
        }

        // 4. Failure status (from M2 description format)
        f[4] = EventStatusExtractor.isFailure(event.getDescription()) ? 1.0 : 0.0;

        // 5. Action Encoded
        if (event.getAction() != null) {
            f[5] = switch (event.getAction().trim().toUpperCase()) {
                case "READ" -> 1.0;
                case "WRITE", "MODIFY" -> 2.0;
                case "COPY" -> 3.0;
                case "EXPORT" -> 4.0;
                case "DELETE" -> 5.0;
                case "EXECUTE" -> 6.0;
                default -> 7.0;
            };
        } else {
            f[5] = 0.0;
        }

        return f;
    }

    /**
     * Determines the most anomalous feature dimension relative to the baseline distribution
     * and produces an explainable forensic description.
     */
    private String buildExplainableReason(
            NormalizedEventDTO event,
            double[] features,
            double[] means,
            double[] stdDevs,
            double rawScore
    ) {
        int maxFeatureIdx = -1;
        double maxZScore = -1.0;

        for (int j = 0; j < features.length; j++) {
            double sd = stdDevs[j];
            double z;
            if (sd > 1e-6) {
                z = Math.abs(features[j] - means[j]) / sd;
            } else {
                z = Math.abs(features[j] - means[j]) > 1e-6 ? 1.0 : 0.0;
            }

            if (z > maxZScore) {
                maxZScore = z;
                maxFeatureIdx = j;
            }
        }

        String explanation;
        switch (maxFeatureIdx) {
            case 0 -> {
                int hour = (event.getTimestamp() != null) ? event.getTimestamp().getHour() : (int) features[0];
                explanation = String.format("unusual timestamp hour (%02d:00) deviating from baseline", hour);
            }
            case 1 -> explanation = String.format("rare or atypical event type '%s' deviating from baseline profile",
                    event.getEventType() != null ? event.getEventType() : "UNKNOWN");
            case 2 -> {
                Long bytes = event.getBytes();
                explanation = String.format("unusually high data transfer volume of %s bytes (log-scale outlier)",
                        bytes != null ? bytes : 0);
            }
            case 3 -> explanation = String.format("unusual port %s deviating from typical network connections",
                    event.getPort() != null ? event.getPort() : "N/A");
            case 4 -> explanation = "failed authentication attempt identified as statistical outlier";
            case 5 -> explanation = String.format("unusual forensic action '%s' deviating from baseline activity",
                    event.getAction() != null ? event.getAction() : "N/A");
            default -> explanation = "multivariate anomaly detected across combined forensic features";
        }

        return String.format("Machine Learning anomaly detected (score: %.3f): %s", rawScore, explanation);
    }

    /**
     * Maps Isolation Forest score in [0.0, 1.0] to standard forensic AnomalySeverity.
     */
    private AnomalySeverity determineSeverity(double score) {
        if (score >= 0.80) {
            return AnomalySeverity.CRITICAL;
        } else if (score >= 0.72) {
            return AnomalySeverity.HIGH;
        } else if (score >= 0.60) {
            return AnomalySeverity.MEDIUM;
        } else {
            return AnomalySeverity.LOW;
        }
    }

    private double roundScore(double score) {
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
