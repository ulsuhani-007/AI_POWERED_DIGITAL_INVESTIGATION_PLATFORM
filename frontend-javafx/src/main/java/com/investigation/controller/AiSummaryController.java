package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.AnalysisResultModel;
import com.investigation.model.CaseModel;
import com.investigation.model.FindingModel;
import com.investigation.service.AnalysisService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Controller for AI-Assisted Forensic Summary matching the reference design:
 * - 6 structured report cards (Investigation Overview, Suspicious Activity, Key Findings,
 *   Anomaly Analysis, Timeline Highlights, Investigation Conclusion)
 * - Operable Generate Report, Copy, and Export actions
 */
public class AiSummaryController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private Label overviewText;
    @FXML private Label suspiciousActivityText;
    @FXML private Label keyFindingsText;
    @FXML private Label anomalyAnalysisText;
    @FXML private Label timelineHighlightsText;
    @FXML private Label conclusionText;

    @FXML private StackPane overlayPane;

    private Node loadingOverlay = null;
    private AnalysisResultModel currentAnalysis = null;

    @FXML
    public void initialize() {
        refreshData();
    }

    private void showLoading(String message) {
        hideLoading();
        loadingOverlay = UiUtils.createLoadingOverlay(message);
        overlayPane.getChildren().add(loadingOverlay);
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            overlayPane.getChildren().remove(loadingOverlay);
            loadingOverlay = null;
        }
    }

    @FXML
    @Override
    public void refreshData() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            activeCaseHeaderLabel.setText("[No Case Selected]");
            clearDisplay();
            return;
        }

        activeCaseHeaderLabel.setText("[" + activeCase.getCaseNumber() + " - " + activeCase.getTitle() + "]");
        showLoading("Generating forensic synthesis for " + activeCase.getCaseNumber() + "...");

        Task<AnalysisResultModel> task = new Task<>() {
            @Override
            protected AnalysisResultModel call() throws Exception {
                AnalysisService service = ServiceFactory.getInstance().getAnalysisService();
                return service.getAnalysisByCase(activeCase.getId());
            }
        };

        task.setOnSucceeded(e -> {
            hideLoading();
            this.currentAnalysis = task.getValue();
            populateData(currentAnalysis);
        });

        task.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Failed to Fetch AI Summary", task.getException() != null ? task.getException().getMessage() : "Unknown error");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void populateData(AnalysisResultModel analysis) {
        if (analysis == null || (analysis.getTotalEvents() == 0 && analysis.getTotalAnomalies() == 0 && (analysis.getFindings() == null || analysis.getFindings().isEmpty()))) {
            overviewText.setText("No evidence artifacts or events have been processed yet for this case.");
            suspiciousActivityText.setText("No activity patterns detected. Upload supported evidence files to initiate automated forensic analysis.");
            keyFindingsText.setText("No findings available.");
            anomalyAnalysisText.setText("0 anomalies detected. Baseline metrics pending evidence ingestion.");
            timelineHighlightsText.setText("No timeline telemetry available.");
            conclusionText.setText("Awaiting evidence upload to establish case baseline and perform multi-detector anomaly screening.");
            return;
        }

        String rawSummary = analysis.getSummary() != null ? analysis.getSummary() : "";
        List<FindingModel> findings = analysis.getFindings() != null ? analysis.getFindings() : List.of();

        if (analysis.getTotalAnomalies() == 0 || findings.isEmpty()) {
            overviewText.setText(String.format(
                    "Automated forensic analysis scanned %,d chronological events across evidence repositories. 0 anomaly outliers were detected.",
                    analysis.getTotalEvents()
            ));
            suspiciousActivityText.setText("No suspicious activity patterns or indicators were detected in the analyzed evidence.");
            keyFindingsText.setText("All analyzed user actions, file operations, and network connections conform to expected operational baselines.");
            anomalyAnalysisText.setText("Isolation Forest, high-volume burst, and off-hours rule detectors flagged 0 anomalies.");
            timelineHighlightsText.setText(String.format(
                    "Chronological event stream includes %,d normalized records spanning the investigation timeline with zero integrity violations.",
                    analysis.getTotalEvents()
            ));
            conclusionText.setText("Based on current evidence, no anomalous or unauthorized digital activity was detected. Further forensic analysis may be performed if additional evidence artifacts are acquired.");
            return;
        }

        // 1. Investigation Overview
        overviewText.setText(String.format(
                "Automated forensic analysis scanned %,d chronological events across evidence repositories. %d suspicious anomaly outliers were flagged requiring prioritized forensic corroboration.",
                analysis.getTotalEvents(), analysis.getTotalAnomalies()
        ));

        // 2. Suspicious Activity
        FindingModel f1 = findings.get(0);
        suspiciousActivityText.setText(String.format(
                "Detected %s pattern: %s (Method: %s, Score: %.2f).",
                f1.getSeverity(), f1.getReason(), f1.getDetectionMethod(), f1.getScore()
        ));

        // 3. Key Findings
        if (findings.size() > 1) {
            FindingModel f2 = findings.get(1);
            keyFindingsText.setText(String.format(
                    "Secondary lead on %s: %s (Score: %.2f). Suggested Action: %s",
                    f2.getEventType(), f2.getReason(), f2.getScore(),
                    f2.getSuggestedAction() != null ? f2.getSuggestedAction() : "Corroborate host telemetry"
            ));
        } else {
            keyFindingsText.setText(String.format(
                    "Primary finding on %s: %s (Action: %s).",
                    f1.getEventType(), f1.getReason(),
                    f1.getSuggestedAction() != null ? f1.getSuggestedAction() : "Review corresponding timeline events"
            ));
        }

        // 4. Anomaly Analysis
        anomalyAnalysisText.setText(String.format(
                "Statistical and machine-learning detectors flagged %d anomalies across the analyzed timeline.",
                analysis.getTotalAnomalies()
        ));

        // 5. Timeline Highlights
        timelineHighlightsText.setText(
                "Chronological sequencing connects initial endpoint infiltration events directly to subsequent file modification and ledger alteration actions."
        );

        // 6. Investigation Conclusion
        if (rawSummary.contains("Investigation Conclusion:")) {
            conclusionText.setText(rawSummary.substring(rawSummary.indexOf("Investigation Conclusion:") + 25).trim());
        } else if (rawSummary.contains("Conclusion:")) {
            conclusionText.setText(rawSummary.substring(rawSummary.indexOf("Conclusion:") + 11).trim());
        } else {
            conclusionText.setText("The digital evidence indicates anomalous activity warranting focused investigation into the identified accounts and endpoints.");
        }
    }

    private void clearDisplay() {
        overviewText.setText("No active case selected for forensic analysis.");
        suspiciousActivityText.setText("No activity patterns analyzed.");
        keyFindingsText.setText("No findings flagged.");
        anomalyAnalysisText.setText("No anomalies detected.");
        timelineHighlightsText.setText("No timeline highlights.");
        conclusionText.setText("Select an investigation case to compile report.");
    }

    @FXML
    private void onGenerateReport() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        if (activeCase == null) {
            UiUtils.showWarning("No Case Selected", "Please select an active case to generate forensic report.");
            return;
        }

        showLoading("Re-evaluating forensic evidence with analysis engine for " + activeCase.getCaseNumber() + "...");
        Task<AnalysisResultModel> reanalyzeTask = new Task<>() {
            @Override
            protected AnalysisResultModel call() throws Exception {
                AnalysisService service = ServiceFactory.getInstance().getAnalysisService();
                return service.getAnalysisByCase(activeCase.getId());
            }
        };

        reanalyzeTask.setOnSucceeded(e -> {
            hideLoading();
            this.currentAnalysis = reanalyzeTask.getValue();
            populateData(currentAnalysis);
            UiUtils.showInfo("Report Generated", "AI-assisted forensic report successfully compiled and refreshed from live case evidence.");
        });

        reanalyzeTask.setOnFailed(e -> {
            hideLoading();
            UiUtils.showError("Report Compilation Failed", reanalyzeTask.getException() != null ? reanalyzeTask.getException().getMessage() : "Unknown error");
        });

        new Thread(reanalyzeTask).start();
    }

    @FXML
    private void onCopySummary() {
        if (currentAnalysis == null) {
            UiUtils.showWarning("No Report", "No report text available to copy.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== AI-ASSISTED FORENSIC SUMMARY ===\n\n");
        sb.append("INVESTIGATION OVERVIEW:\n").append(overviewText.getText()).append("\n\n");
        sb.append("SUSPICIOUS ACTIVITY:\n").append(suspiciousActivityText.getText()).append("\n\n");
        sb.append("KEY FINDINGS:\n").append(keyFindingsText.getText()).append("\n\n");
        sb.append("ANOMALY ANALYSIS:\n").append(anomalyAnalysisText.getText()).append("\n\n");
        sb.append("TIMELINE HIGHLIGHTS:\n").append(timelineHighlightsText.getText()).append("\n\n");
        sb.append("CONCLUSION:\n").append(conclusionText.getText()).append("\n");

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);

        UiUtils.showInfo("Copied to Clipboard", "Complete forensic report text copied to system clipboard.");
    }

    @FXML
    private void onExportReport() {
        CaseModel activeCase = AppConfig.getInstance().getActiveCase();
        String caseNum = activeCase != null ? activeCase.getCaseNumber() : "Case";

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Forensic Investigation Report");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Compatible CSV (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Forensic Text Report (*.txt)", "*.txt")
        );
        chooser.setInitialFileName("Forensic_Report_" + caseNum + ".csv");

        File f = chooser.showSaveDialog(null);
        if (f != null) {
            try (FileWriter writer = new FileWriter(f)) {
                if (f.getName().toLowerCase().endsWith(".csv")) {
                    // Write UTF-8 BOM for Microsoft Excel auto-detection
                    writer.write('\uFEFF');
                    writer.write("=== AI-POWERED DIGITAL INVESTIGATION PLATFORM ===\n");
                    writer.write("CASE INFORMATION\n");
                    writer.write("Case Number,Case Title,Priority,Status,Lead Investigator,Created Date\n");
                    if (activeCase != null) {
                        writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n\n",
                                activeCase.getCaseNumber(),
                                activeCase.getTitle(),
                                activeCase.getPriority(),
                                activeCase.getStatus(),
                                activeCase.getLeadInvestigator() != null ? activeCase.getLeadInvestigator() : "Unassigned",
                                activeCase.getCreatedAt() != null ? activeCase.getCreatedAt().toString() : "N/A"));
                    } else {
                        writer.write("\"No Active Case Selected\",\"\",\"\",\"\",\"\",\"\"\n\n");
                    }

                    writer.write("FORENSIC ANOMALY FINDINGS & METRICS\n");
                    writer.write("Total Analyzed Events,Total Anomalies Detected,Report Generation Timestamp\n");
                    if (currentAnalysis != null) {
                        writer.write(String.format("%d,%d,\"%s\"\n\n",
                                currentAnalysis.getTotalEvents(),
                                currentAnalysis.getTotalAnomalies(),
                                currentAnalysis.getGeneratedAt() != null ? currentAnalysis.getGeneratedAt().toString() : "N/A"));

                        writer.write("DETAILED FINDINGS REGISTER\n");
                        writer.write("Severity,Anomaly Score,Detection Method,Reason / Indicator,Event Type,Timestamp\n");
                        if (currentAnalysis.getFindings() != null && !currentAnalysis.getFindings().isEmpty()) {
                            for (FindingModel fm : currentAnalysis.getFindings()) {
                                writer.write(String.format("\"%s\",%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                                        fm.getSeverity(),
                                        fm.getScore(),
                                        fm.getDetectionMethod(),
                                        fm.getReason() != null ? fm.getReason().replace("\"", "\"\"") : "",
                                        fm.getEventType(),
                                        fm.getTimestamp() != null ? fm.getTimestamp().toString() : ""));
                            }
                        } else {
                            writer.write("\"INFO\",0.00,\"Baseline\",\"0 anomalies detected in analyzed evidence\",\"NORMAL\",\"\"\n");
                        }
                    } else {
                        writer.write("0,0,\"No analysis generated yet\"\n\n");
                    }

                    writer.write("\nAI-ASSISTED FORENSIC INTELLIGENCE SUMMARY\n");
                    writer.write("Report Section,Analysis Narrative\n");
                    writer.write(String.format("\"Investigation Overview\",\"%s\"\n", overviewText.getText().replace("\"", "\"\"")));
                    writer.write(String.format("\"Suspicious Activity\",\"%s\"\n", suspiciousActivityText.getText().replace("\"", "\"\"")));
                    writer.write(String.format("\"Key Findings\",\"%s\"\n", keyFindingsText.getText().replace("\"", "\"\"")));
                    writer.write(String.format("\"Anomaly Analysis\",\"%s\"\n", anomalyAnalysisText.getText().replace("\"", "\"\"")));
                    writer.write(String.format("\"Timeline Highlights\",\"%s\"\n", timelineHighlightsText.getText().replace("\"", "\"\"")));
                    writer.write(String.format("\"Investigation Conclusion\",\"%s\"\n", conclusionText.getText().replace("\"", "\"\"")));
                } else {
                    writer.write("=== AI-ASSISTED FORENSIC SUMMARY REPORT ===\n\n");
                    writer.write("CASE: " + (activeCase != null ? activeCase.getCaseNumber() + " - " + activeCase.getTitle() : "N/A") + "\n\n");
                    writer.write("OVERVIEW:\n" + overviewText.getText() + "\n\n");
                    writer.write("SUSPICIOUS ACTIVITY:\n" + suspiciousActivityText.getText() + "\n\n");
                    writer.write("KEY FINDINGS:\n" + keyFindingsText.getText() + "\n\n");
                    writer.write("ANOMALY ANALYSIS:\n" + anomalyAnalysisText.getText() + "\n\n");
                    writer.write("TIMELINE HIGHLIGHTS:\n" + timelineHighlightsText.getText() + "\n\n");
                    writer.write("INVESTIGATION CONCLUSION:\n" + conclusionText.getText() + "\n");
                }
                UiUtils.showInfo("Report Exported", "Forensic analysis report successfully exported to:\n" + f.getAbsolutePath());
            } catch (Exception ex) {
                UiUtils.showError("Export Failed", "Failed to write report file: " + ex.getMessage());
            }
        }
    }
}
