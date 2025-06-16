package com.example.pupsis_main_dashboard.utilities;

import com.example.pupsis_main_dashboard.controllers.GeneralNotificationController;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationUtil {


    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationUtil.class);

    public static void show(Stage ownerStage, String message, String type, String position) {
        String fxmlPath = switch (type.toLowerCase()) {
            case "success" -> "/com/example/pupsis_main_dashboard/fxml/SuccessNotificationBar.fxml";
            case "error" -> "/com/example/pupsis_main_dashboard/fxml/ErrorNotificationBar.fxml";
            case "info" -> "/com/example/pupsis_main_dashboard/fxml/InfoNotificationBar.fxml";
            case "warning" -> "/com/example/pupsis_main_dashboard/fxml/WarningNotificationBar.fxml";
            default -> null;
        };

        if (fxmlPath == null) {
            System.err.println("Unknown notification type: " + type);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(NotificationUtil.class.getResource(fxmlPath));
            HBox root = loader.load();

            GeneralNotificationController controller = loader.getController();
            controller.setMessage(message);

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoFix(true);
            popup.setAutoHide(false);
            popup.setHideOnEscape(true);

            // --- Positioning Logic ---
            double x = 0;
            double y = 0;
            Scene scene = ownerStage.getScene();

            switch (position.toLowerCase()) {
                case "top-right" -> {
                    x = ownerStage.getX() + scene.getWidth() - root.getPrefWidth() - 30;
                    y = ownerStage.getY() + 30;
                }
                case "top-center" -> {
                    x = ownerStage.getX() + (scene.getWidth() / 2) - (root.getPrefWidth() / 2) + 30;
                    y = ownerStage.getY() + 30;
                }
                case "top-left" -> {
                    x = ownerStage.getX() + 30;
                    y = ownerStage.getY() + 30;
                }
                case "bottom-right" -> {
                    x = ownerStage.getX() + scene.getWidth() - root.getPrefWidth() - 45;
                    y = ownerStage.getY() + scene.getHeight() - root.getPrefHeight() - 35;
                }
                case "bottom-center" -> {
                    x = ownerStage.getX() + (scene.getWidth() / 2) - (root.getPrefWidth() / 2);
                    y = ownerStage.getY() + scene.getHeight() - root.getPrefHeight() - 30;
                }
                case "bottom-left" -> {
                    x = ownerStage.getX() + 30;
                    y = ownerStage.getY() + scene.getHeight() - root.getPrefHeight() - 30;
                }
                case "center" -> {
                    x = ownerStage.getX() + (scene.getWidth() / 2) - (root.getPrefWidth() / 2);
                    y = ownerStage.getY() + (scene.getHeight() / 2) - (root.getPrefHeight() / 2);
                }
                default -> {
                    // Fallback to top-right
                    x = ownerStage.getX() + scene.getWidth() - root.getPrefWidth() - 30;
                    y = ownerStage.getY() + 30;
                }
            }
            popup.show(ownerStage, x, y);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(5), new KeyValue(root.opacityProperty(), 1)),
                    new KeyFrame(Duration.seconds(2), new KeyValue(root.opacityProperty(), 1)),
                    new KeyFrame(Duration.seconds(3), e -> popup.hide(), new KeyValue(root.opacityProperty(), 0))
            );
            timeline.setRate(0.5);
            timeline.play();

        } catch (Exception e) {
            logger.error("Failed to show notification", e);
        }
    }
}
