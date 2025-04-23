package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class VerificationController {
    @FXML private TextField digit1;
    @FXML private TextField digit2;
    @FXML private TextField digit3;
    @FXML private TextField digit4;
    @FXML private TextField digit5;
    @FXML private TextField digit6;
    @FXML private Label errorLabel;
    @FXML private Label infoMessage;
    
    private String expectedCode;
    private Stage stage;
    private Runnable onSuccessCallback;
    
    public void initialize() {
        setupDigitField(digit1, digit2);
        setupDigitField(digit2, digit3);
        setupDigitField(digit3, digit4);
        setupDigitField(digit4, digit5);
        setupDigitField(digit5, digit6);
        setupDigitField(digit6, null);
        
        // Add key listeners
        digit1.setOnKeyPressed(this::handleKeyPress);
        digit2.setOnKeyPressed(this::handleKeyPress);
        digit3.setOnKeyPressed(this::handleKeyPress);
        digit4.setOnKeyPressed(this::handleKeyPress);
        digit5.setOnKeyPressed(this::handleKeyPress);
        digit6.setOnKeyPressed(this::handleKeyPress);
    }
    
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.BACK_SPACE) {
            handleBackspace(event);
        } else if (event.getCode() == KeyCode.ENTER) {
            handleVerification();
        }
    }
    
    private void handleBackspace(KeyEvent event) {
        TextField currentField = (TextField) event.getSource();
        if (currentField.getText().isEmpty()) {
            // Move focus to previous field and clear it
            if (currentField == digit6) digit5.requestFocus();
            else if (currentField == digit5) digit4.requestFocus();
            else if (currentField == digit4) digit3.requestFocus();
            else if (currentField == digit3) digit2.requestFocus();
            else if (currentField == digit2) digit1.requestFocus();
            
            // Clear the previous field
            TextField previousField = getPreviousField(currentField);
            if (previousField != null) {
                previousField.clear();
                previousField.requestFocus();
            }
        }
    }
    
    private TextField getPreviousField(TextField current) {
        if (current == digit2) return digit1;
        if (current == digit3) return digit2;
        if (current == digit4) return digit3;
        if (current == digit5) return digit4;
        if (current == digit6) return digit5;
        return null;
    }
    
    private void setupDigitField(TextField current, TextField next) {
        // Only allow single digits
        current.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().length() > 1) {
                return null;
            }
            if (!change.getControlNewText().matches("[0-9]*")) {
                return null;
            }
            return change;
        }));
        
        // Autofocus to next field when digit entered
        if (next != null) {
            current.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() == 1) {
                    next.requestFocus();
                }
            });
        }
    }
    
    public void initializeVerification(String expectedCode, String userEmail, Stage stage, Runnable onSuccessCallback) {
        if (expectedCode == null || expectedCode.length() != 6) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        this.expectedCode = expectedCode;
        this.stage = stage;
        this.onSuccessCallback = onSuccessCallback;
        
        // Set masked email message
        String maskedEmail = maskEmail(userEmail);
//        String maskedEmail = "gratedestroyer99@gmail.com";
        infoMessage.setText("Enter verification code sent to " + maskedEmail);
    }
    
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "**********@gmail.com";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 3) {
            return "********" + email.substring(atIndex);
        }
        return email.substring(0, 3) + "*****" + email.substring(atIndex);
    }
    
    @FXML
    private void handleVerification() {
        String enteredCode = digit1.getText() + digit2.getText() + digit3.getText() + 
                           digit4.getText() + digit5.getText() + digit6.getText();
        
        if (enteredCode.length() != 6) {
            errorLabel.setText("Please enter all 6 digits");
            return;
        }
        
        if (!enteredCode.equals(expectedCode)) {
            errorLabel.setText("Invalid verification code");
            return;
        }
        
        errorLabel.setText("");
        stage.close();
        if (onSuccessCallback != null) {
            onSuccessCallback.run();
        }
    }
    
    @FXML
    private void handleResendCode() {
        // Implement resend code logic here
        digit1.requestFocus();
        digit1.clear();
        digit2.clear();
        digit3.clear();
        digit4.clear();
        digit5.clear();
        digit6.clear();
        errorLabel.setText("New verification code sent");
    }
}
