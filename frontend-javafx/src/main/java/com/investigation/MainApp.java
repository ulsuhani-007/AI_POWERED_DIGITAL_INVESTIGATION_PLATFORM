package com.investigation;

import com.investigation.controller.LoginController;
import com.investigation.service.SessionService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Main JavaFX Application entrypoint for the AI-Powered Digital Investigation Platform.
 * Enforces authentication by presenting the Login screen before accessing the workstation.
 */
public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        primaryStage.setTitle("AEGIS // AI-Powered Digital Investigation Workstation");
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(700);

        // Window icon if available
        try (InputStream iconStream = getClass().getResourceAsStream("/com/investigation/icons/app_icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        // Wire unauthorized / logout handler to return to the login screen
        SessionService.getInstance().setUnauthorizedHandler(this::showLoginScreen);

        if (SessionService.getInstance().isLoggedIn()) {
            showMainLayout();
        } else {
            showLoginScreen();
        }

        primaryStage.show();
    }

    public void showLoginScreen() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/investigation/view/LoginView.fxml"));
                Parent loginRoot = loader.load();
                LoginController controller = loader.getController();

                controller.setOnLoginSuccess(session -> {
                    showMainLayout();
                });

                Scene scene = new Scene(loginRoot, 1280, 820);
                scene.getStylesheets().add(getClass().getResource("/com/investigation/css/theme.css").toExternalForm());
                primaryStage.setScene(scene);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void showMainLayout() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/investigation/view/MainLayout.fxml"));
                Parent mainRoot = loader.load();

                Scene scene = new Scene(mainRoot, 1280, 820);
                scene.getStylesheets().add(getClass().getResource("/com/investigation/css/theme.css").toExternalForm());
                primaryStage.setScene(scene);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
