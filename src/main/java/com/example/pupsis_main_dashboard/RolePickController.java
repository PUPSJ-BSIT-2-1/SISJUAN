package com.example.pupsis_main_dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class RolePickController {

    @FXML
    private void handleStudentButton(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("StudentLoginPage.fxml"));
        Stage newStage = new Stage();
        newStage.setScene(new Scene(root));
        newStage.centerOnScreen();
        newStage.setResizable(false);
        newStage.show();
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }

    @FXML
    private void handleFacultyButton(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("FacultyAndAdminLoginPage.fxml"));
        Stage newStage = new Stage();
        newStage.setScene(new Scene(root));
        newStage.centerOnScreen();
        newStage.setResizable(false);
        newStage.show();
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }
}