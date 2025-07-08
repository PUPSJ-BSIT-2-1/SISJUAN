package com.sisjuan.controllers;

import com.sisjuan.models.TransactionHistory;
import com.sisjuan.utilities.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

public class AdminPaymentTransactionController {

    @FXML
    private TableView<TransactionHistory> paymentTransactionTable;
    @FXML private TableColumn<TransactionHistory, String> transactionIDColumn;
    @FXML private TableColumn<TransactionHistory, String> dateTimeColumn;
    @FXML private TableColumn<TransactionHistory, String> studentNumberColumn;
    @FXML private TableColumn<TransactionHistory, String> paymentMethodColumn;
    @FXML private TableColumn<TransactionHistory, String> amountColumn;
    @FXML private TableColumn<TransactionHistory, String> assessmentColumn;
    @FXML private TableColumn<TransactionHistory, String> balanceColumn;
    @FXML private TableColumn<TransactionHistory, String> statusColumn;

    @FXML private Label generatedOnLabel;

    private static final Logger logger = LoggerFactory.getLogger(AdminPaymentTransactionController.class.getName());

    private final ObservableList<TransactionHistory> paymentTransactionList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        var columns = List.of(transactionIDColumn, dateTimeColumn, studentNumberColumn, paymentMethodColumn, amountColumn, assessmentColumn, balanceColumn, statusColumn);
        columns.forEach(col -> {
            col.setReorderable(false);
            col.setSortable(false);
        });

        generatedOnLabel.setText("Generated on: " + LocalDate.now());

        paymentTransactionTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        transactionIDColumn.setCellValueFactory(cellData -> cellData.getValue().transactionIDProperty());
        dateTimeColumn.setCellValueFactory(cellData -> cellData.getValue().dateTimeProperty());
        studentNumberColumn.setCellValueFactory(cellData -> cellData.getValue().studentNumberProperty());
        paymentMethodColumn.setCellValueFactory(cellData -> cellData.getValue().paymentMethodProperty());
        amountColumn.setCellValueFactory(cellData -> cellData.getValue().amountProperty());
        assessmentColumn.setCellValueFactory(cellData -> cellData.getValue().assessmentProperty());
        balanceColumn.setCellValueFactory(cellData -> cellData.getValue().balanceProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        loadTransactionHistory();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) paymentTransactionTable.getScene().getWindow();
        stage.close();
    }

    private void loadTransactionHistory() {
        paymentTransactionList.clear();

        String sql = "SELECT * FROM payments ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                paymentTransactionList.add(new TransactionHistory(
                        rs.getString("transaction_id"),
                        rs.getString("created_at"),
                        rs.getString("student_number"),
                        rs.getString("payment_source"),
                        String.valueOf(rs.getDouble("amount")),
                        String.valueOf(rs.getDouble("assessment")),
                        String.valueOf(rs.getDouble("balance")),
                        rs.getString("status")
                ));
            }

            paymentTransactionTable.refresh();
            paymentTransactionTable.setItems(paymentTransactionList);
        } catch (Exception e) {
            logger.error("Error loading transaction history: {}", e.getMessage());
        }

        paymentTransactionTable.setItems(paymentTransactionList);
    }

    public static void showPaymentTransactionHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(AdminPaymentTransactionController.class.getResource("/com/sisjuan/fxml/AdminPaymentTransaction.fxml"));
            BorderPane root = loader.load();

            AdminPaymentTransactionController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
            dialogStage.showAndWait();
        } catch (IOException e) {
            logger.error("Error loading payment transaction history: {}", e.getMessage());
        }
    }

}
