package com.example.pupsis_main_dashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;

import java.io.IOException;

public class PUPSIS extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) throws IOException {FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource("StudentLoginPage.fxml"));
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
        stage.getIcons().add(new Image(getClass().getResource("/com/example/pupsis_main_dashboard/pupsj-logo.png").toExternalForm()));
        stage.setScene(new Scene(root, javafx.scene.paint.Color.TRANSPARENT));
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}