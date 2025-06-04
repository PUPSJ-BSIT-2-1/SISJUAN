package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class PayBalanceController {

    @FXML private RadioButton gcashBtn;
    @FXML private RadioButton cardBtn;
    @FXML private RadioButton bankBtn;
    @FXML private TextField amountField;
    @FXML private Button submitPaymentBtn;
    @FXML private Label errorLabel;
    @FXML private Button backBtn;

    @FXML private HBox cardContainer;
    @FXML private HBox bankContainer;
    @FXML private HBox gcashContainer;

    @FXML private Label balanceLabel;

    private ToggleGroup paymentMethodGroup;
    private PaymentTrailController paymentTrailController;
    private double currentBalance;

    public void setPaymentTrailController(PaymentTrailController controller) {
        this.paymentTrailController = controller;
    }

    public void setCurrentBalance(double balance) {
        this.currentBalance = balance;
        updateBalanceLabel();
    }

    private void updateBalanceLabel() {
        if (balanceLabel != null) {
            balanceLabel.setText("Current Balance: ₱" + String.format("%,.2f", currentBalance));
        }
    }

    @FXML
    public void initialize() {
        paymentMethodGroup = new ToggleGroup();
        cardBtn.setToggleGroup(paymentMethodGroup);
        bankBtn.setToggleGroup(paymentMethodGroup);
        gcashBtn.setToggleGroup(paymentMethodGroup);

        submitPaymentBtn.setOnAction(e -> handlePayment());
    }

    private void handlePayment() {
        Toggle selectedToggle = paymentMethodGroup.getSelectedToggle();

        if (selectedToggle == null) {
            errorLabel.setText("Please select a payment method.");
            return;
        }

        String method = ((RadioButton) selectedToggle).getText();
        String amountText = amountField.getText();

        if (amountText.isEmpty()) {
            errorLabel.setText("Please enter an amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                errorLabel.setText("Amount must be greater than zero.");
                return;
            }

            boolean success = paymentTrailController.processPayment(method, amount);
            if (success) {
                errorLabel.setText("");
                showAlert(Alert.AlertType.INFORMATION, "Payment Successful", "You paid ₱" + amount + " via " + method);
                paymentTrailController.showPaymentTrail();

                Stage stage = (Stage) submitPaymentBtn.getScene().getWindow();
                stage.close();
            } else {
                errorLabel.setText("Payment exceeds current balance.");
            }

        } catch (NumberFormatException ex) {
            errorLabel.setText("Invalid amount entered.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        if (paymentTrailController != null) {
            paymentTrailController.showPaymentTrail();
        }

        Stage stage = (Stage) backBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void selectCard() {
        cardBtn.setSelected(true);
        highlightSelected(cardContainer);
    }

    @FXML
    private void selectBank() {
        bankBtn.setSelected(true);
        highlightSelected(bankContainer);
    }

    @FXML
    private void selectGCash() {
        gcashBtn.setSelected(true);
        highlightSelected(gcashContainer);
    }

    private void highlightSelected(HBox selectedBox) {
        cardContainer.getStyleClass().remove("payment-card-selected");
        bankContainer.getStyleClass().remove("payment-card-selected");
        gcashContainer.getStyleClass().remove("payment-card-selected");

        selectedBox.getStyleClass().add("payment-card-selected");
    }
}
