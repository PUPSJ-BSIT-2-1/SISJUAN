package com.example.pupsis_main_dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class RolePickController {

    @FXML
    private void handleLoginButton(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("StudentLoginPage.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();
    }

}
