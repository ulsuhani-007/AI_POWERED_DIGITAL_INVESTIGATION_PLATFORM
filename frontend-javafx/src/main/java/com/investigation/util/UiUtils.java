package com.investigation.util;

import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;
import com.investigation.model.enums.Severity;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.Optional;

/**
 * Common UI helper utilities for creating badges, alerts, dialogs, thumbnails, and states
 * strictly styled according to the investigation platform reference design.
 */
public final class UiUtils {

    private UiUtils() {}

    public static Label createBadge(String text, String textColorHex, String bgColorHex, String borderColorHex) {
        Label label = new Label(text);
        label.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 3px 10px; " +
                        "-fx-background-radius: 12px; -fx-border-color: %s; -fx-border-radius: 12px; " +
                        "-fx-border-width: 1px; -fx-font-size: 11px; -fx-font-weight: bold;",
                bgColorHex, textColorHex, borderColorHex
        ));
        return label;
    }

    public static Label createStatusBadge(CaseStatus status) {
        if (status == null) status = CaseStatus.OPEN;
        return switch (status) {
            case IN_PROGRESS -> createBadge("In Progress", "#B45309", "#FEF3C7", "#FDE68A");
            case CLOSED -> createBadge("Analyzed", "#047857", "#D1FAE5", "#A7F3D0");
            case ARCHIVED -> createBadge("Archived", "#475569", "#F1F5F9", "#CBD5E1");
            case OPEN -> createBadge("Under Review", "#0369A1", "#E0F2FE", "#BAE6FD");
        };
    }

    public static Label createPriorityBadge(CasePriority priority) {
        if (priority == null) priority = CasePriority.MEDIUM;
        return switch (priority) {
            case CRITICAL, HIGH -> createBadge(priority.getDisplayName(), "#B91C1C", "#FEE2E2", "#FECACA");
            case MEDIUM -> createBadge(priority.getDisplayName(), "#B45309", "#FEF3C7", "#FDE68A");
            case LOW -> createBadge(priority.getDisplayName(), "#047857", "#D1FAE5", "#A7F3D0");
        };
    }

    public static Label createSeverityBadge(Severity severity) {
        if (severity == null) severity = Severity.INFO;
        return switch (severity) {
            case CRITICAL, HIGH -> createBadge(severity.getDisplayName(), "#B91C1C", "#FEE2E2", "#FECACA");
            case MEDIUM -> createBadge(severity.getDisplayName(), "#B45309", "#FEF3C7", "#FDE68A");
            case LOW -> createBadge(severity.getDisplayName(), "#0369A1", "#E0F2FE", "#BAE6FD");
            case INFO -> createBadge(severity.getDisplayName(), "#475569", "#F1F5F9", "#CBD5E1");
        };
    }

    public static boolean isHistoricalCase(String caseNumber) {
        if (caseNumber == null) return false;
        String s = caseNumber.toUpperCase();
        return s.contains("HIST") || s.contains("LUFTHANSA") || s.contains("MUNDHRA") ||
               s.contains("BHOPAL") || s.contains("HARSHAD") || s.contains("LINDBERGH") || s.contains("MONALISA");
    }

    public static Label createHistoricalBadge() {
        return createBadge("HISTORICAL REFERENCE / DEMONSTRATION DATA", "#4338CA", "#EEF2FF", "#C7D2FE");
    }

    public static Image getCaseThumbnail(String caseNumber, String title) {
        String num = caseNumber != null ? caseNumber.toUpperCase() : "";
        String t = title != null ? title.toUpperCase() : "";

        String path = null;
        if (num.contains("LUFTHANSA") || t.contains("LUFTHANSA")) {
            path = "/com/investigation/images/lufthansa_thumb.jpg";
        } else if (num.contains("MUNDHRA") || t.contains("MUNDHRA")) {
            path = "/com/investigation/images/mundhra_thumb.jpg";
        } else if (num.contains("BHOPAL") || t.contains("BHOPAL")) {
            path = "/com/investigation/images/bhopal_thumb.jpg";
        } else if (num.contains("HARSHAD") || t.contains("HARSHAD")) {
            path = "/com/investigation/images/harshad_mehta_thumb.jpg";
        } else if (num.contains("LINDBERGH") || t.contains("LINDBERGH")) {
            path = "/com/investigation/images/lindbergh_thumb.jpg";
        } else if (num.contains("MONALISA") || num.contains("MONA") || t.contains("MONA")) {
            path = "/com/investigation/images/mona_lisa_thumb.jpg";
        }

        if (path == null) {
            return null;
        }

        try {
            InputStream is = UiUtils.class.getResourceAsStream(path);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Node createCaseThumbnailView(String caseNumber, String title, double width, double height, double radius) {
        Image img = getCaseThumbnail(caseNumber, title);
        if (img == null) {
            String initial = (title != null && !title.isBlank()) ? title.substring(0, Math.min(2, title.length())).toUpperCase() : "CS";
            Label iconLabel = new Label("📁 " + initial);
            iconLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
            StackPane placeholder = new StackPane(iconLabel);
            placeholder.setPrefSize(width, height);
            placeholder.setMaxSize(width, height);
            placeholder.setMinSize(width, height);
            placeholder.setStyle(String.format("-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-border-radius: %fpx; -fx-background-radius: %fpx;", radius, radius));
            return placeholder;
        }

        ImageView iv = new ImageView(img);
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        iv.setPreserveRatio(false);

        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        iv.setClip(clip);

        return iv;
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane pane = alert.getDialogPane();
        pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #E2E8F0; -fx-border-width: 1px;");
        for (ButtonType bt : pane.getButtonTypes()) {
            Button btn = (Button) pane.lookupButton(bt);
            if (btn != null) {
                if (bt == ButtonType.OK || bt == ButtonType.YES) {
                    btn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 6px 14px;");
                } else {
                    btn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #334155; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6px 14px;");
                }
            }
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && (result.get() == ButtonType.OK || result.get() == ButtonType.YES);
    }

    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Action Notice");
            alert.setContentText(message);
            DialogPane pane = alert.getDialogPane();
            pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #FECACA;");
            alert.showAndWait();
        });
    }

    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane pane = alert.getDialogPane();
            pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #E2E8F0;");
            alert.showAndWait();
        });
    }

    public static void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane pane = alert.getDialogPane();
            pane.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #FDE68A;");
            alert.showAndWait();
        });
    }

    public static VBox createLoadingOverlay(String message) {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(44, 44);
        indicator.setStyle("-fx-progress-color: #2563EB;");

        Label label = new Label(message != null ? message : "Processing digital forensics intelligence...");
        label.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox box = new VBox(16, indicator, label);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.88); -fx-padding: 30px;");
        return box;
    }

    public static VBox createEmptyState(String iconSymbol, String title, String subtitle) {
        Label icon = new Label(iconSymbol != null ? iconSymbol : "📂");
        icon.setStyle("-fx-font-size: 32px; -fx-text-fill: #94A3B8;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        subLabel.setWrapText(true);

        VBox box = new VBox(10, icon, titleLabel, subLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }
}
