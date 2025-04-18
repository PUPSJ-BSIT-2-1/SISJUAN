package com.example.pupsis_main_dashboard;

import com.example.utility.Utils;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class PUPSIS extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        Utils utility = new Utils();

        try {
            Stage initializedStage = utility.loadScene(
                    "fxml/StudentLogin.fxml",
                    "PUPSIS",
                    getClass().getResource("/com/example/pupsis_main_dashboard/Images/pupsj-logo.png").toExternalForm()
            );

            initializedStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}