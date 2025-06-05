package com.example.pupsis_main_dashboard.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class PaymentHistoryController {

    @FXML
    private Button backButton;

    @FXML
    private TableView<TransactionRecord> transactionTable;

    @FXML
    private TableColumn<TransactionRecord, LocalDate> dateColumn;

    @FXML
    private TableColumn<TransactionRecord, String> descriptionColumn;

    @FXML
    private TableColumn<TransactionRecord, String> orNumberColumn;

    @FXML
    private TableColumn<TransactionRecord, Double> debitColumn;

    @FXML
    private TableColumn<TransactionRecord, Double> creditColumn;

    @FXML
    private TableColumn<TransactionRecord, Double> balanceColumn;

    private PaymentTrailController paymentPageController;

    private final ObservableList<TransactionRecord> transactions = FXCollections.observableArrayList();

    public void setPaymentPageController(PaymentTrailController controller) {
        this.paymentPageController = controller;
    }

    @FXML
    public void initialize() {
        backButton.setOnAction(event -> {
            if (paymentPageController != null) {
                paymentPageController.showPaymentTrail();
            }
        });


         dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        orNumberColumn.setCellValueFactory(new PropertyValueFactory<>("orNumber"));
        debitColumn.setCellValueFactory(new PropertyValueFactory<>("debit"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("credit"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        transactionTable.setItems(transactions);
    }

    public void addTransaction(LocalDate date, String description, String orNumber, double debit, double credit, double balance) {
        TransactionRecord record = new TransactionRecord(date, description, orNumber, debit, credit, balance);
        transactions.add(record);
    }
}
