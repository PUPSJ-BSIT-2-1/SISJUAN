package com.example.pupsis_main_dashboard.controllers;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PayBalanceWindow extends Stage {

    private final PaymentTrailController parentController;

    public PayBalanceWindow(PaymentTrailController parent) {
        this.parentController = parent;

        setTitle("Pay Balance");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        Label instructionLabel = new Label("Select Payment Method:");

        ToggleGroup paymentMethods = new ToggleGroup();

        RadioButton gcashBtn = new RadioButton("Gcash");
        gcashBtn.setToggleGroup(paymentMethods);

        RadioButton cardBtn = new RadioButton("Card");
        cardBtn.setToggleGroup(paymentMethods);

        RadioButton bankBtn = new RadioButton("Bank");
        bankBtn.setToggleGroup(paymentMethods);

        Label amountLabel = new Label("Amount to Pay:");
        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");

        Button payBtn = new Button("Pay");
        payBtn.setDefaultButton(true);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        payBtn.setOnAction(e -> {
            RadioButton selectedMethod = (RadioButton) paymentMethods.getSelectedToggle();
            if (selectedMethod == null) {
                errorLabel.setText("Please select a payment method.");
                return;
            }
            String method = selectedMethod.getText();

            double amount;
            try {
                amount = Double.parseDouble(amountField.getText());
                if (amount <= 0) {
                    errorLabel.setText("Amount must be greater than zero.");
                    return;
                }
            } catch (NumberFormatException ex) {
                errorLabel.setText("Invalid amount entered.");
                return;
            }

            // Inform parent controller about the payment made
            parentController.processPayment(method, amount);

            // Close this window
            this.close();
        });

        root.getChildren().addAll(instructionLabel, gcashBtn, cardBtn, bankBtn, amountLabel, amountField, payBtn, errorLabel);

        Scene scene = new Scene(root, 300, 300);
        setScene(scene);

        initModality(Modality.APPLICATION_MODAL);
    }
}
