package com.example.pupsis_main_dashboard;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class PUPSIS extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource("FrontPage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("PUPSIS");
		stage.getIcons().add(new Image(getClass().getResource("/com/example/pupsis_main_dashboard/pupsj-logo.png").toExternalForm()));
        applySceneWithTransition(root, stage);
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();
    }

    public void applySceneWithTransition(Parent root, Stage stage) {
        Scene newScene = new Scene(root);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        stage.setScene(newScene);
    }

    public static void main(String[] args) {
        launch();
    }
}