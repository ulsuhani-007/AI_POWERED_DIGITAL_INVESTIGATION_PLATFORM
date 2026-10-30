package com.investigation.controller;

import com.investigation.config.AppConfig;
import com.investigation.model.AnalysisResultModel;
import com.investigation.model.CaseModel;
import com.investigation.model.FindingModel;
import com.investigation.service.AnalysisService;
import com.investigation.service.ServiceFactory;
import com.investigation.util.DateTimeUtils;
import com.investigation.util.UiUtils;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

public class AiSummaryController implements MainLayoutController.Refreshable {

    @FXML private Label activeCaseHeaderLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalAnomaliesLabel;
    @FXML private Label riskScoreLabel;
    @FXML private Label primaryVectorLabel;

    @FXML private TextArea summaryTextArea;
    @FXML private HBox statsContainer;
    @FXML private VBox findingsCardsContainer;

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
            try {
                List<CaseModel> allCases = ServiceFactory.getInstance().getCaseService().getAllCases();
                if (!allCases.isEmpty()) {
                    activeCase = allCases.get(0);
                    AppConfig.getInstance().setActiveCase(activeCase);
                }
            } catch (Exception ignored) {}
        }

        if (activeCase == null) {
            activeCaseHeaderLabel.setText("NO ACTIVE CASE SELECTED");
            clearDisplay();
            return;
        }

        final CaseModel currentCase = activeCase;
        activeCaseHeaderLabel.setText("AI INVESTIGATIVE INTELLIGENCE SUMMARY FOR " + currentCase.getCaseNumber() + " : " + currentCase.getTitle());

        showLoading("Generating AI synthesis report (GET /api/cases/" + currentCase.getId() + "/analysis)...");

        Task<AnalysisResultModel> task = new Task<>() {
            @Override
            protected AnalysisResultModel call() throws Exception {
                AnalysisService service = ServiceFactory.getInstance().getAnalysisService();
                return service.getAnalysisByCase(currentCase.getId());
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
        if (analysis == null) {
            clearDisplay();
            return;
        }

        totalEventsLabel.setText(String.format("%,d", analysis.getTotalEvents()));
        totalAnomaliesLabel.setText(String.valueOf(analysis.getTotalAnomalies()));

        // Check stats for threat vector and risk
        Map<String, Object> stats = analysis.getStatistics();
        if (stats != null) {
            Object maxScore = stats.get("max_anomaly_score");
            riskScoreLabel.setText(maxScore != null ? String.valueOf(maxScore) : "N/A");

            Object vector = stats.get("primary_threat_vector");
            primaryVectorLabel.setText(vector != null ? String.valueOf(vector) : "Multi-Stage Activity");

            // Populate statistics pills
            statsContainer.getChildren().clear();
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                HBox pill = new HBox(6);
                pill.setAlignment(Pos.CENTER_LEFT);
                pill.setStyle("-fx-background-color: #0F172A; -fx-padding: 6px 12px; -fx-background-radius: 6px; -fx-border-color: #1E293B; -fx-border-radius: 6px;");

                Label key = new Label(formatKey(entry.getKey()) + ":");
                key.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

                Label val = new Label(String.valueOf(entry.getValue()));
                val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

                pill.getChildren().addAll(key, val);
                statsContainer.getChildren().add(pill);
            }
        }

        // Summary text
        summaryTextArea.setText(analysis.getSummary() != null && !analysis.getSummary().isBlank()
                ? analysis.getSummary()
                : "No summary text generated by AI service.");

        // Key Findings & Investigative Leads cards
        findingsCardsContainer.getChildren().clear();
        if (analysis.getFindings() != null && !analysis.getFindings().isEmpty()) {
            for (FindingModel finding : analysis.getFindings()) {
                findingsCardsContainer.getChildren().add(createFindingLeadCard(finding));
            }
        } else {
            findingsCardsContainer.getChildren().add(new Label("No anomalous leads detected."));
        }
    }

    private VBox createFindingLeadCard(FindingModel f) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #0D1424; -fx-padding: 12px 14px; -fx-background-radius: 6px; -fx-border-color: #1E293B; -fx-border-radius: 6px;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        HBox badgeBox = new HBox();
        badgeBox.getChildren().add(UiUtils.createSeverityBadge(f.getSeverity()));

        Label reasonLabel = new Label(f.getReason());
        reasonLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");
        reasonLabel.setWrapText(true);
        HBox.setHgrow(reasonLabel, Priority.ALWAYS);

        Label scoreBadge = new Label("Score: " + String.format("%.2f", f.getScore()));
        scoreBadge.setStyle("-fx-font-size: 10px; -fx-font-family: monospace; -fx-text-fill: #00F0FF; -fx-background-color: rgba(0, 240, 255, 0.1); -fx-padding: 2px 6px; -fx-background-radius: 4px;");

        top.getChildren().addAll(badgeBox, reasonLabel, scoreBadge);

        HBox meta = new HBox(16);
        Label eventRef = new Label("Supporting Event: " + (f.getEventId() != null ? f.getEventId() : "N/A"));
        eventRef.setStyle("-fx-font-size: 10px; -fx-font-family: monospace; -fx-text-fill: #64748B;");

        Label method = new Label("Engine: " + f.getDetectionMethod());
        method.setStyle("-fx-font-size: 10px; -fx-text-fill: #A855F7;");

        Label time = new Label("Time: " + DateTimeUtils.formatUtc(f.getTimestamp()));
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");

        meta.getChildren().addAll(eventRef, method, time);

        if (f.getSuggestedAction() != null && !f.getSuggestedAction().isBlank()) {
            HBox actionBox = new HBox(6);
            actionBox.setStyle("-fx-background-color: rgba(56, 189, 248, 0.08); -fx-padding: 6px 10px; -fx-background-radius: 4px;");
            Label leadIcon = new Label("🎯 LEAD:");
            leadIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");
            Label leadText = new Label(f.getSuggestedAction());
            leadText.setStyle("-fx-font-size: 11px; -fx-text-fill: #38BDF8;");
            leadText.setWrapText(true);
            actionBox.getChildren().addAll(leadIcon, leadText);
            card.getChildren().addAll(top, meta, actionBox);
        } else {
            card.getChildren().addAll(top, meta);
        }

        return card;
    }

    private void clearDisplay() {
        totalEventsLabel.setText("--");
        totalAnomaliesLabel.setText("--");
        riskScoreLabel.setText("--");
        primaryVectorLabel.setText("--");
        summaryTextArea.clear();
        if (statsContainer != null) {
            statsContainer.getChildren().clear();
        }
        if (findingsCardsContainer != null) {
            findingsCardsContainer.getChildren().clear();
        }
    }

    private String formatKey(String key) {
        if (key == null) return "";
        return key.replace("_", " ").toUpperCase();
    }

    @FXML
    private void onCopySummary() {
        if (summaryTextArea.getText() != null && !summaryTextArea.getText().isBlank()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(summaryTextArea.getText());
            clipboard.setContent(content);
            UiUtils.showInfo("Copied to Clipboard", "Executive investigative summary copied to system clipboard.");
        }
    }
}
