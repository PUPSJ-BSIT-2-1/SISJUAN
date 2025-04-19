package com.example.pupsis_main_dashboard;

import com.example.utility.Utils;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Test extends Application {

    @Override
    public void start(Stage stage) {
        Utils utility = new Utils();

        try {
            Stage initializedStage = utility.loadScene(
                    "fxml/StudentLogin.fxml",
                    "PUPSIS",
                    Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/Images/pupsj-logo.png")).toExternalForm()
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