package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

public class PaymentTrailController {

    // Payment Method
    @FXML private RadioButton cardRadio;
    @FXML private RadioButton bankRadio;
    @FXML private RadioButton gcashRadio;

    // Payment Summary
    @FXML private TextField amountPaidField;
    @FXML private TextField totalFeesField;
    @FXML private TextField changeDueField;
    @FXML private Button submitPaymentButton;

    // Fee Breakdown Labels
    @FXML private Label tuitionFeeLabel;
    @FXML private Label labFeeLabel;
    @FXML private Label libraryFeeLabel;
    @FXML private Label regFeeLabel;
    @FXML private Label idFeeLabel;
    @FXML private Label miscFeeLabel;
    @FXML private Label totalAmountDueLabel;

    // Balance (optional)
    @FXML private Label balanceLabel;

    private double currentBalance = 0.00;

    @FXML
    public void initialize() {
        // Group the radio buttons
        ToggleGroup paymentGroup = new ToggleGroup();
        cardRadio.setToggleGroup(paymentGroup);
        bankRadio.setToggleGroup(paymentGroup);
        gcashRadio.setToggleGroup(paymentGroup);

        // Placeholder for future setup of fee breakdown and total balance
        // (to be implemented with dynamic data from backend/database)

        updateBalanceLabel();

        // Handle payment submission
        submitPaymentButton.setOnAction(event -> handleSubmitPayment());
    }

    private void handleSubmitPayment() {
        String method = getSelectedPaymentMethod();
        if (method == null) {
            System.out.println("Please select a payment method.");
            return;
        }

        try {
            String amountStr = amountPaidField.getText().replace("₱", "").replace(",", "").trim();
            double amountPaid = Double.parseDouble(amountStr);

            if (amountPaid <= 0 || amountPaid > currentBalance) {
                System.out.println("Invalid payment amount.");
                return;
            }

            double change = 0.0;
            currentBalance -= amountPaid;

            if (currentBalance < 0) {
                change = -currentBalance;
                currentBalance = 0;
            }

            changeDueField.setText(String.format("₱%,.2f", change));

            updateBalanceLabel();

            System.out.println("Payment successful via " + method);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format.");
        }
    }

    private String getSelectedPaymentMethod() {
        if (cardRadio.isSelected()) return "Credit/Debit Card";
        if (bankRadio.isSelected()) return "Bank Transfer";
        if (gcashRadio.isSelected()) return "GCash";
        return null;
    }

    private void updateBalanceLabel() {
        if (balanceLabel != null) {
            balanceLabel.setText(String.format("Current Balance: ₱%,.2f", currentBalance));
        }
    }
}