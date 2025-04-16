package com.example.pupsis_main_dashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;

public class PUPSIS extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PUPSIS.class.getResource("FrontPage.fxml"));
        Parent root = fxmlLoader.load();
        stage.setTitle("PUPSIS");
        stage.getIcons().add(new Image(getClass().getResource("/com/example/pupsis_main_dashboard/pupsj-logo.png").toExternalForm()));
        stage.setScene(new Scene(root));
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}