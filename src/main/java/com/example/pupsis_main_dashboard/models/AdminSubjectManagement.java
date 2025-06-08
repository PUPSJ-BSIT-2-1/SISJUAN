package com.example.pupsis_main_dashboard.models;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminSubjectManagement extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/subjectmodule/fxml/AdminSubjectModule.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Subject Module");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

}