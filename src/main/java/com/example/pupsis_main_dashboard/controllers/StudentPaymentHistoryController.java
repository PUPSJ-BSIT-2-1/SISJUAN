package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.PaymentHistory;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class StudentPaymentHistoryController {

    Logger logger = LoggerFactory.getLogger(StudentPaymentHistoryController.class.getName());

    @FXML
    private TableView<PaymentHistory> transactionTable;
    @FXML
    private TableColumn<PaymentHistory, String> schoolYearColumn;
    @FXML
    private TableColumn<PaymentHistory, Button> semesterColumn;
    @FXML
    private TableColumn<PaymentHistory, String> descriptionColumn;
    @FXML
    private TableColumn<PaymentHistory, String> orDateColumn;
    @FXML
    private TableColumn<PaymentHistory, String> assessmentColumn;
    @FXML
    private TableColumn<PaymentHistory, String> creditColumn;
    @FXML
    private TableColumn<PaymentHistory, String> balanceColumn;

    @FXML
    private Button backButton;
    private ScrollPane contentPane;

    @FXML
    public void initialize() {
        logger.info("Initializing StudentPaymentHistoryController.");

        schoolYearColumn.setCellValueFactory(new PropertyValueFactory<>("schoolYear"));
        semesterColumn.setCellValueFactory(new PropertyValueFactory<>("semesterButton"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("paymentSource"));
        orDateColumn.setCellValueFactory(new PropertyValueFactory<>("orDate"));
        assessmentColumn.setCellValueFactory(new PropertyValueFactory<>("assessment"));
        creditColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        var columns = new TableColumn[]{schoolYearColumn, semesterColumn, descriptionColumn, orDateColumn, assessmentColumn, creditColumn, balanceColumn};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        backButton.setOnMouseClicked(_ -> {
            handleBackToDashboard();
            resetScrollPosition();
        });

        loadTransactionHistory();
    }

    private void loadTransactionHistory() {
        logger.info("Loading transaction history.");

        String studentNumber = SessionData.getInstance().getStudentNumber();

        transactionTable.getItems().clear();

        ObservableList<PaymentHistory> historyList = FXCollections.observableArrayList();

        String query = """
                  SELECT
                  ay.academic_year_name,
                  sem.semester_name,
                  p.payment_source ,
                  p.created_at,
                  p.approved_at,
                  p.assessment,
                  p.amount,
                  p.balance
                FROM payments p
                JOIN students s ON p.student_number = s.student_number
                JOIN section sec ON s.current_year_section_id = sec.section_id
                JOIN semesters sem ON sec.semester_id = sem.semester_id
                JOIN academic_years ay ON sem.academic_year_id = ay.academic_year_id
                WHERE p.student_number = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Button semesterButton = new Button(rs.getString("semester_name"));
                    semesterButton.getStyleClass().add("semester-button");
                    String schoolYear = rs.getString("academic_year_name");
                    String description = rs.getString("payment_source");
                    String createdDate = rs.getDate("created_at").toLocalDate().toString();
                    String approvedDate = rs.getDate("approved_at").toLocalDate().toString();
                    Double assessment = Double.valueOf(rs.getString("assessment"));
                    Double amount = Double.valueOf(rs.getString("amount"));
                    Double balance = Double.valueOf(rs.getString("balance"));

                    historyList.add(new PaymentHistory(schoolYear, semesterButton, "TOTAL AMOUNT DUE", createdDate, assessment, 0.00, assessment));
                    historyList.add(new PaymentHistory(null, null, description, approvedDate, balance, amount, balance));

                    transactionTable.setItems(historyList);
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading transaction history: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error loading transaction history: {}", e.getMessage());
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            // Find the ScrollPane with fx:id "contentPane" in the current scene
            contentPane = (ScrollPane) transactionTable.getScene().lookup("#contentPane");

            if (contentPane != null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/pupsis_main_dashboard/fxml/StudentPaymentInfo.fxml")
                );

                Parent newContent = loader.load();

                StudentPaymentInfoController controller = loader.getController();

                contentPane.setContent(newContent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetScrollPosition() {
        Platform.runLater(() -> {
            contentPane.setVvalue(0);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> contentPane.setVvalue(0));
                }
            }, 100); // 100ms delay for final layout
        });
    }

}
