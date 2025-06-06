package com.example.pupsis_main_dashboard.utilities;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class PaymentTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/PaymentTrail.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("Payment Trail Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
