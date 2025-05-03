package com.example.pupsis_main_dashboard;

import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class PUPSIS extends Application {

    @Override
    public void start(Stage stage) {
        StageAndSceneUtils utility = new StageAndSceneUtils();

        try {
            Stage initializedStage = utility.loadStage(
                    "fxml/FrontPage.fxml",
                    "PUPSIS",
                    Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/Images/PUPSJ Logo.png")).toExternalForm(),
                    StageAndSceneUtils.WindowSize.MEDIUM
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