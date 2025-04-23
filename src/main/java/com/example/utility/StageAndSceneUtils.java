package com.example.utility;

import com.example.pupsis_main_dashboard.PUPSIS;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

@SuppressWarnings("ALL")
public class StageAndSceneUtils {

    private double xOffset = 0;
    private double yOffset = 0;

    // Standard window sizes
    public enum WindowSize { SMALL, MEDIUM, LARGE }
    private static final double SMALL_WIDTH = 600;
    private static final double SMALL_HEIGHT = 400;
    private static final double MEDIUM_WIDTH = 1280;
    private static final double MEDIUM_HEIGHT = 720;
    private static final double LARGE_WIDTH = 1280;
    private static final double LARGE_HEIGHT = 720;

    public void loadStage(Stage stage, String fxmlFile, WindowSize size) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource(fxmlFile));
        Parent root = fxmlLoader.load();

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        double width = size == WindowSize.MEDIUM ? MEDIUM_WIDTH : size == WindowSize.LARGE ? LARGE_WIDTH : SMALL_WIDTH;
        double height = size == WindowSize.MEDIUM ? MEDIUM_HEIGHT : size == WindowSize.LARGE ? LARGE_HEIGHT : SMALL_HEIGHT;

        Scene scene = new Scene(root, width, height, javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(700), root);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    public Stage loadStage(String fxmlFile, String title, String iconPath, WindowSize size) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource(fxmlFile));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.centerOnScreen();

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle(title);
        if (iconPath != null) {
            stage.getIcons().add(new javafx.scene.image.Image(iconPath));
        }

        double width = size == WindowSize.MEDIUM ? MEDIUM_WIDTH : SMALL_WIDTH;
        double height = size == WindowSize.MEDIUM ? MEDIUM_HEIGHT : SMALL_HEIGHT;

        stage.setScene(new Scene(root, width, height, javafx.scene.paint.Color.TRANSPARENT));
        stage.setResizable(false);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(700), root);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();

        return stage;
    }

    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

}