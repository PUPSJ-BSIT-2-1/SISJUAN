package com.example.pupsis_main_dashboard;


import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

public class PUPSIS extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource("/com/example/pupsis_main_dashboard/fxml/FrontPage.fxml"));
        Parent root = fxmlLoader.load();

        // Make window draggable
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("PUPSIS");
        stage.getIcons().add(new Image(getClass().getResource("/com/example/pupsis_main_dashboard/Images/pupsj-logo.png").toExternalForm()));
        stage.setScene(new Scene(root, javafx.scene.paint.Color.TRANSPARENT));
        stage.centerOnScreen();
        stage.setResizable(false);
        FadeTransition fadeTransition = new FadeTransition();
        fadeTransition.setDuration(Duration.millis(700)); // Animation duration: 1000ms (1 second)
        fadeTransition.setNode(root); // Apply to the root node
        fadeTransition.setFromValue(0.0); // Start fully transparent
        fadeTransition.setToValue(1.0); // End fully visible
        fadeTransition.play(); // Start the animation
        stage.show();


    }

    public static void main(String[] args) {
        launch();
    }
}