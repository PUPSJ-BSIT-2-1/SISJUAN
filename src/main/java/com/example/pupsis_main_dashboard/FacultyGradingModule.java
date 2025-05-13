package com.example.GradingModule;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import javafx.stage.StageStyle;

public class FacultyGradingModule extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlLocation = FacultyGradingModule.class.getResource("/fxml/newGradingModule.fxml");
        if (fxmlLocation == null) {
            throw new IOException("Cannot find FXML file: /fxml/newGradingModule.fxml");
        }
        
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Faculty Grading Module");
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(null);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}