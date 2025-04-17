package com.example.pupsis_main_dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class FrontPageController {

    @FXML
    private void handleGetStarted(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RolePick.fxml"));
            Parent root = fxmlLoader.load();
            Stage newStage = new Stage();
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            newStage.setScene(scene);
            newStage.centerOnScreen();
            newStage.setResizable(false);
            newStage.initStyle(StageStyle.TRANSPARENT);

            newStage.show();
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
