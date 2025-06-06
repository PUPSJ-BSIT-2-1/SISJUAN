package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalDate;

public class PaymentTrailController {

    @FXML private StackPane contentPane;
    @FXML private Button viewPaymentHistoryBtn;
    @FXML private Button payBalanceBtn;
    @FXML private Label balanceLabel;

    private Node paymentTrailContent;
    private Node paymentHistoryContent;
    private PaymentHistoryController historyController;

    private double currentBalance = 5000.00;

    @FXML
    public void initialize() {
        paymentTrailContent = contentPane.getChildren().get(0);

        try {
            FXMLLoader historyLoader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/PaymentHistory.fxml"));
            paymentHistoryContent = historyLoader.load();
            historyController = historyLoader.getController();
            historyController.setPaymentPageController(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateBalanceLabel();

        viewPaymentHistoryBtn.setOnAction(event -> {
            contentPane.getChildren().setAll(paymentHistoryContent);
            viewPaymentHistoryBtn.setVisible(false);
            payBalanceBtn.setVisible(false);
        });

        payBalanceBtn.setOnAction(event -> openPayBalancePopup());
    }

    public void showPaymentTrail() {
        contentPane.getChildren().setAll(paymentTrailContent);
        viewPaymentHistoryBtn.setVisible(true);
        payBalanceBtn.setVisible(true);
    }

    public boolean processPayment(String method, double amount) {
        if (amount > currentBalance) {
            return false;
        }

        currentBalance -= amount;
        if (currentBalance < 0) currentBalance = 0;

        updateBalanceLabel();

        if (historyController != null) {
            historyController.addTransaction(
                    LocalDate.now(),
                    "Paid via " + method,
                    "AUTO-GEN",
                    0,
                    amount,
                    currentBalance
            );
        }

        return true;
    }

    private void updateBalanceLabel() {
        if (balanceLabel != null) {
            balanceLabel.setText(String.format("Current Balance: ₱%,.2f", currentBalance));
        }
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    private void openPayBalancePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/PayBalance.fxml"));
            Parent root = loader.load();

            PayBalanceController controller = loader.getController();
            controller.setPaymentTrailController(this);
            controller.setCurrentBalance(currentBalance); // <-- Important: sync balance

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setResizable(false);

            int width = 500;
            int height = 600;
            stage.setWidth(width);
            stage.setHeight(height);

            double centerX = (Screen.getPrimary().getBounds().getWidth() - width) / 2;
            double centerY = (Screen.getPrimary().getBounds().getHeight() - height) / 2;
            stage.setX(centerX);
            stage.setY(centerY);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
