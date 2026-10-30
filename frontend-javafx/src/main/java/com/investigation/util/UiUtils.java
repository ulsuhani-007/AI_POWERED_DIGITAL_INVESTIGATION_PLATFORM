package com.investigation.util;

import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;
import com.investigation.model.enums.Severity;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Optional;

/**
 * Common UI helper utilities for creating badges, alerts, dialogs, and states.
 */
public final class UiUtils {

    private UiUtils() {}

    public static Label createBadge(String text, String colorHex, String bgDarkHex) {
        Label label = new Label(text);
        label.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 3px 8px; " +
                        "-fx-background-radius: 12px; -fx-border-color: %s; -fx-border-radius: 12px; " +
                        "-fx-border-width: 1px; -fx-font-size: 10px; -fx-font-weight: bold;",
                bgDarkHex, colorHex, colorHex
        ));
        return label;
    }

    public static Label createStatusBadge(CaseStatus status) {
        if (status == null) status = CaseStatus.OPEN;
        return createBadge(status.getDisplayName(), status.getColorHex(), "#0F172A");
    }

    public static Label createPriorityBadge(CasePriority priority) {
        if (priority == null) priority = CasePriority.MEDIUM;
        return createBadge(priority.getDisplayName(), priority.getColorHex(), "#0F172A");
    }

    public static Label createSeverityBadge(Severity severity) {
        if (severity == null) severity = Severity.INFO;
        return createBadge(severity.getDisplayName(), severity.getBadgeColorHex(), severity.getBgDarkHex());
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Style the dialog pane with dark theme
        DialogPane pane = alert.getDialogPane();
        pane.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #F8FAFC;");
        for (ButtonType bt : pane.getButtonTypes()) {
            Button btn = (Button) pane.lookupButton(bt);
            if (btn != null) {
                btn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #38BDF8; -fx-border-color: #334155;");
            }
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Action Failed");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static VBox createLoadingOverlay(String message) {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(40, 40);

        Label label = new Label(message != null ? message : "Loading data from backend...");
        label.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox box = new VBox(15, indicator, label);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(9, 13, 22, 0.85); -fx-padding: 30px;");
        return box;
    }

    public static VBox createEmptyState(String iconSymbol, String title, String subtitle) {
        Label icon = new Label(iconSymbol != null ? iconSymbol : "📂");
        icon.setStyle("-fx-font-size: 32px; -fx-text-fill: #475569;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        subLabel.setWrapText(true);

        VBox box = new VBox(8, icon, titleLabel, subLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }
}
