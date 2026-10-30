package com.investigation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Main JavaFX Application entrypoint for the AI-Powered Digital Investigation Platform.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/investigation/view/MainLayout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(getClass().getResource("/com/investigation/css/theme.css").toExternalForm());

        primaryStage.setTitle("AEGIS // AI-Powered Digital Investigation Workstation");
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(700);

        // Window icon if available
        try (InputStream iconStream = getClass().getResourceAsStream("/com/investigation/icons/app_icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception ignored) {}

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
