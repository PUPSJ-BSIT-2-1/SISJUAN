package com.example.pupsis_main_dashboard.controllers;

import com.example.utility.StageAndSceneUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class VerificationController {
    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    
    private String expectedCode;
    private String email;
    private Stage stage;
    private Runnable onVerificationSuccess;
    
    public void initialize(String email, String expectedCode, Stage stage, Runnable onVerificationSuccess) {
        this.email = email;
        this.expectedCode = expectedCode;
        this.stage = stage;
        this.onVerificationSuccess = onVerificationSuccess;
    }
    
    @FXML
    private void handleVerification() {
        String enteredCode = codeField.getText().trim();
        
        if (enteredCode.isEmpty()) {
            errorLabel.setText("Please enter the verification code");
            return;
        }
        
        if (!enteredCode.equals(expectedCode)) {
            errorLabel.setText("Invalid verification code");
            return;
        }
        
        onVerificationSuccess.run();
        stage.close();
    }
    
    @FXML
    private void handleResendCode() {
        // Implement resend logic here
        errorLabel.setText("New code sent to " + email);
    }
}
