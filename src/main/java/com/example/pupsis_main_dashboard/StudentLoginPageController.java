package com.example.pupsis_main_dashboard;

import javafx.animation.FadeTransition;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

public class StudentLoginPageController {
    private String[] usernames = {"Harold"};
    private String[] passwords = {"Hello123"};

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel; // Label to display error messages

    @FXML
    private void handleKeyPressOnUsername(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            passwordField.requestFocus();
        }
    }

    @FXML
    private void handleKeyPressOnPassword(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLoginButton(new javafx.event.ActionEvent(event.getSource(), null));
        }
    }

    @FXML
    private void handleLoginButton(javafx.event.ActionEvent event) {
        int sw = 0;
        int i = 0;
        for (String username : usernames) {
            if (usernameField != null && passwordField != null
                    && usernameField.getText().equals(username)) {
                if (i < passwords.length && passwordField.getText().equals(passwords[i])) {
                    sw = 1;
                    try {
                        Parent root = FXMLLoader.load(getClass().getResource("MainDashboard.fxml"));
                        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.centerOnScreen();
                        stage.setResizable(false);
                        stage.show();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            i++;
        }
        if (sw == 0) {
            errorLabel.setText("Invalid username or password."); // Display error message
            passwordField.setText("");
            usernameField.setText("");
            usernameField.requestFocus();
        } else {
            errorLabel.setText(""); // Clear error message on successful login
        }
    }
    @FXML
    private void handleBackButton(MouseEvent event) throws java.io.IOException {
        Parent root = FXMLLoader.load(getClass().getResource("RolePick.fxml"));
        Stage newStage = new Stage();
        newStage.setScene(new Scene(root));
        newStage.centerOnScreen();
        newStage.setResizable(false);
        newStage.show();
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close();
    }
}
