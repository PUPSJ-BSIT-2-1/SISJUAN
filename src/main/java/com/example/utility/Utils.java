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
public class Utils {

    private double xOffset = 0;
    private double yOffset = 0;

    public void loadScene(Stage stage, String fxmlFile) throws IOException {
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

        Scene scene = new Scene(root, javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);

        FadeTransition fadeTransition = new FadeTransition(Duration.millis(700), root);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();
    }

    public Stage loadScene(String fxmlFile, String title, String iconPath) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource(fxmlFile));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();

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
        stage.setScene(new Scene(root, javafx.scene.paint.Color.TRANSPARENT));
        stage.centerOnScreen();
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