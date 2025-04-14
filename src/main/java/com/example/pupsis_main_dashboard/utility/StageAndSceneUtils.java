package com.example.pupsis_main_dashboard.utility;

import com.example.pupsis_main_dashboard.PUPSIS;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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

    // Standard window sizes
    public enum WindowSize { SMALL, MEDIUM, LARGE }
    private static final double SMALL_WIDTH = 800;
    private static final double SMALL_HEIGHT = 600;
    private static final double MEDIUM_WIDTH = 1280;
    private static final double MEDIUM_HEIGHT = 720;
    private static final double LARGE_WIDTH = 1600;
    private static final double LARGE_HEIGHT = 900;

    public enum TransitionType {
        FADE,
        SLIDE_RIGHT,
        SLIDE_LEFT,
        SLIDE_UP,
        SLIDE_DOWN,
        ZOOM_IN
    }

    public void loadStage(Stage stage, String fxmlFile, WindowSize size) throws IOException {
        loadStage(stage, fxmlFile, size, TransitionType.FADE);
    }

    public void loadStage(Stage stage, String fxmlFile, WindowSize size, TransitionType transitionType) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource(fxmlFile));
            Parent root = fxmlLoader.load();

            double width = size == WindowSize.MEDIUM ? MEDIUM_WIDTH : size == WindowSize.LARGE ? LARGE_WIDTH : SMALL_WIDTH;
            double height = size == WindowSize.MEDIUM ? MEDIUM_HEIGHT : size == WindowSize.LARGE ? LARGE_HEIGHT : SMALL_HEIGHT;

            Scene scene = new Scene(root, width, height, javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.centerOnScreen();

            applyTransition(root, transitionType);
        } catch (IOException e) {
            showAlert("Error", "Failed to load view: " + e.getMessage(), Alert.AlertType.ERROR);
            throw e;
        }
    }

    public Stage loadStage(String fxmlFile, String title, String iconPath, WindowSize size) throws IOException {
        return loadStage(fxmlFile, title, iconPath, size, TransitionType.FADE);
    }

    public Stage loadStage(String fxmlFile, String title, String iconPath, WindowSize size, TransitionType transitionType) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource(fxmlFile));
            Parent root = fxmlLoader.load();
            
            Stage stage = new Stage();
            stage.centerOnScreen();

            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle(title);
            if (iconPath != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconPath));
            }

            double width = size == WindowSize.MEDIUM ? MEDIUM_WIDTH : size == WindowSize.LARGE ? LARGE_WIDTH : SMALL_WIDTH;
            double height = size == WindowSize.MEDIUM ? MEDIUM_HEIGHT : size == WindowSize.LARGE ? LARGE_HEIGHT : SMALL_HEIGHT;

            stage.setScene(new Scene(root, width, height, javafx.scene.paint.Color.TRANSPARENT));
            stage.setResizable(false);

            applyTransition(root, transitionType);

            return stage;
        } catch (IOException e) {
            showAlert("Error", "Failed to load view: " + e.getMessage(), Alert.AlertType.ERROR);
            throw e;
        }
    }

    private void applyTransition(Parent root, TransitionType transitionType) {
        final int TRANSITION_DURATION = 700;
        
        switch (transitionType) {

            case FADE:
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(TRANSITION_DURATION), root);
                fadeTransition.setFromValue(0.0);
                fadeTransition.setToValue(1.0);
                fadeTransition.setInterpolator(Interpolator.EASE_BOTH);
                fadeTransition.play();
                break;
                
            case SLIDE_RIGHT:
                TranslateTransition slideRight = new TranslateTransition(Duration.millis(TRANSITION_DURATION), root);
                slideRight.setFromX(-root.getScene().getWidth());
                slideRight.setToX(0);
                slideRight.setInterpolator(Interpolator.EASE_BOTH);
                slideRight.play();
                break;
                
            case SLIDE_LEFT:
                TranslateTransition slideLeft = new TranslateTransition(Duration.millis(TRANSITION_DURATION), root);
                slideLeft.setFromX(root.getScene().getWidth());
                slideLeft.setToX(0);
                slideLeft.setInterpolator(Interpolator.EASE_BOTH);
                slideLeft.play();
                break;
                
            case SLIDE_UP:
                TranslateTransition slideUp = new TranslateTransition(Duration.millis(TRANSITION_DURATION), root);
                slideUp.setFromY(root.getScene().getHeight());
                slideUp.setToY(0);
                slideUp.setInterpolator(Interpolator.EASE_BOTH);
                slideUp.play();
                break;
                
            case SLIDE_DOWN:
                TranslateTransition slideDown = new TranslateTransition(Duration.millis(TRANSITION_DURATION), root);
                slideDown.setFromY(-root.getScene().getHeight());
                slideDown.setToY(0);
                slideDown.setInterpolator(Interpolator.EASE_BOTH);
                slideDown.play();
                break;
                
            case ZOOM_IN:
                ScaleTransition zoomIn = new ScaleTransition(Duration.millis(TRANSITION_DURATION), root);
                zoomIn.setFromX(0.5);
                zoomIn.setFromY(0.5);
                zoomIn.setToX(1.0);
                zoomIn.setToY(1.0);
                zoomIn.setInterpolator(Interpolator.EASE_BOTH);
                zoomIn.play();
                break;
        }
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

    public static void clearCache() {
        // Clear any FXMLLoader caches or stage references if needed
        // Currently no specific caches to clear, but method is added for future use
    }

}