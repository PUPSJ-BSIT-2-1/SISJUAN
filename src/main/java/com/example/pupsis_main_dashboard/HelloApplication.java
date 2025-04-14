package com.example.pupsis_main_dashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hello!");
        stage.setScene(scene);
//        stage.initStyle(StageStyle.TRANSPARENT);
//        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}