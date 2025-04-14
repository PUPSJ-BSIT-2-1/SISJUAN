package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.EmailService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.Objects;

public class VerificationCodeController {
    @FXML private TextField digit1, digit2, digit3, digit4, digit5, digit6;
    @FXML private Label errorLabel, infoMessage;
    private final EmailService emailService = new EmailService();
    private String expectedCode;
    private Stage stage;
    private Runnable onSuccessCallback;

    public void initialize() {
        TextField[] digits = {digit1, digit2, digit3, digit4, digit5, digit6};
        for (int i = 0; i < digits.length; i++) {
            setupDigitField(digits[i], i < digits.length-1 ? digits[i+1] : null);
            digits[i].setOnKeyPressed(this::handleKeyPress);
            digits[i].addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.isControlDown() && e.getCode() == KeyCode.V) {
                    handlePasteFromClipboard();
                    e.consume();
                }
            });
        }
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getCode() == KeyCode.BACK_SPACE) handleBackspace(e);
        else if (e.getCode() == KeyCode.ENTER) handleVerification();
    }

    private void handleBackspace(KeyEvent e) {
        TextField current = (TextField) e.getSource();
        if (current.getText().isEmpty() && getPreviousField(current) != null) {
            assert getPreviousField(current) != null;
            Objects.requireNonNull(getPreviousField(current)).clear();
            Objects.requireNonNull(getPreviousField(current)).requestFocus();
        }
    }

    private TextField getPreviousField(TextField current) {
        TextField[] digits = {digit1, digit2, digit3, digit4, digit5, digit6};
        for (int i = 1; i < digits.length; i++)
            if (current == digits[i]) return digits[i-1];
        return null;
    }

    private void setupDigitField(TextField current, TextField next) {
        current.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().length() > 1 || !c.getControlNewText().matches("[0-9]*") ? null : c
        ));
        if (next != null) current.textProperty().addListener((_, _, val) -> {
            if (val.length() == 1) next.requestFocus();
        });
    }

    public void initializeVerification(String code, String email, Stage stage, Runnable callback) {
        this.expectedCode = code;
        this.stage = stage;
        this.onSuccessCallback = callback;
        infoMessage.setText("Enter verification code sent to " + maskEmail(email));
    }

    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "**********@gmail.com";
        int at = email.indexOf('@');
        return at <= 3 ? "********" + email.substring(at) : email.substring(0, 3) + "*****" + email.substring(at);
    }

    private void handlePasteFromClipboard() {
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (text != null && text.matches("\\d{6}")) {
                char[] digits = text.toCharArray();
                for (int i = 0; i < 6; i++) ((TextField) getClass().getDeclaredField("digit"+(i+1)).get(this)).setText(String.valueOf(digits[i]));
                digit6.requestFocus();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML private void handleVerification() {
        String code = digit1.getText()+digit2.getText()+digit3.getText()+digit4.getText()+digit5.getText()+digit6.getText();
        if (code.length() != 6) errorLabel.setText("Please enter all 6 digits");
        else if (!code.equals(expectedCode)) errorLabel.setText("Invalid verification code");
        else { stage.close(); if (onSuccessCallback != null) onSuccessCallback.run(); }
    }

    @FXML private void handleResendCode() {
        String newCode = String.format("%06d", (int)(Math.random() * 1000000));
        String email = infoMessage.getText().substring(infoMessage.getText().lastIndexOf(" ") + 1);
        new Thread(() -> {
            try {
                emailService.sendVerificationEmail(email, newCode);
                javafx.application.Platform.runLater(() -> {
                    TextField[] digits = {digit1, digit2, digit3, digit4, digit5, digit6};
                    for (TextField d : digits) d.clear();
                    digit1.requestFocus();
                    errorLabel.setText("New verification code sent");
                    expectedCode = newCode;
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> errorLabel.setText("Failed to send email"));
            }
        }).start();
    }
}