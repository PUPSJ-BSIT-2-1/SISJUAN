package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Payment;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class AdminStudentPaymentManagementController {


    private static final Logger logger = LoggerFactory.getLogger(AdminStudentPaymentManagementController.class);

    @FXML private VBox studentPaymentContainer;
    @FXML private HBox viewTransactionHistory;
    private VBox paymentContainer;

    
    private final List<Payment> currentDisplayedPayments = new ArrayList<>();

    @FXML
    private void initialize() {
        logger.info("Initializing AdminStudentPaymentManagementController...");

        paymentContainer = new VBox();
        paymentContainer.setSpacing(5);


        if (studentPaymentContainer != null) {
            studentPaymentContainer.getChildren().clear(); // Clear the FXML container
            studentPaymentContainer.getChildren().add(paymentContainer); // Add header and the studentList VBox directly
        } else {
            // This is a critical FXML loading/injection issue if this happens.
            logger.error("CRITICAL FXML ERROR: studentListContainer is null. " +
                         "Check AdminStudentManagement.fxml for a VBox with fx:id=\"studentListContainer\". " +
                         "UI will not load correctly.");
            // As a fallback, to prevent crashes if other parts of the code expect studentList to be non-null,
            // ensure it's initialized, though it won't be visible.
            if (this.paymentContainer == null) { // Should have been initialized above, but as a safeguard.
                this.paymentContainer = new VBox();
            }
        }

        viewTransactionHistory.setOnMouseClicked(_ -> AdminPaymentTransactionController.showPaymentTransactionHistory());

        loadPendingStudentPayments();
        logger.info("AdminStudentManagementController initialized.");
    }

    private void loadPendingStudentPayments() {

        logger.info("Loading pending student payments...");
        currentDisplayedPayments.clear();

        new Thread(() -> {
            List<Payment> pendingPayments = new ArrayList<>();
            String getPendingSql = """
            SELECT
              p.payment_id,
              p.transaction_id,
              p.student_number,
              s.firstname,
              s.lastname,
              fhe.status_name,
              sec.section_name,
              sem.semester_name,
              ay.academic_year_name,
              p.balance,
              p.amount,
              p.assessment,
              p.status,
              p.payment_source,
              p.created_at,
              p.approved_at
            FROM payments p
            JOIN students s ON p.student_number = s.student_number
            JOIN fhe_act_statuses fhe ON fhe.fhe_id = s.fhe_eligible_id
            JOIN section sec ON s.current_year_section_id = sec.section_id
            JOIN semesters sem ON sec.semester_id = sem.semester_id
            JOIN academic_years ay ON sem.academic_year_id = ay.academic_year_id
            WHERE p.status = 'Pending';
                """;

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(getPendingSql);
                 ResultSet rs = pstmt.executeQuery()) {

                logger.debug("Executing query to fetch pending students: {}", getPendingSql);
                while (rs.next()) {
                    Payment payment = new Payment(
                            rs.getInt("payment_id"),
                            rs.getString("transaction_id"),
                            rs.getString("student_number"),
                            rs.getString("firstname"),
                            rs.getString("lastname"),
                            rs.getString("section_name"),
                            rs.getString("semester_name"),
                            rs.getString("academic_year_name"),
                            rs.getDouble("balance"),
                            rs.getDouble("amount"),
                            rs.getDouble("assessment"),
                            rs.getString("status"),
                            rs.getString("payment_source"),
                            rs.getString("created_at"),
                            rs.getString("approved_at"),
                            rs.getString("status_name")
                    );

                    pendingPayments.add(payment);
                    currentDisplayedPayments.add(payment);
                    logger.debug("Loaded pending student payment: {}", rs.getInt("payment_id"));
                }
            } catch (SQLException e) {
                logger.error("SQL Error loading pending students: {}", e.getMessage(), e);
                Platform.runLater(() -> StageAndSceneUtils.showAlert("Database Error", "Failed to load pending students."));
            }

            Platform.runLater(() -> {
                paymentContainer.getChildren().clear();
                if (pendingPayments.isEmpty()) {
                    Label noPaymentsLabel = new Label("No pending student payments found.");
                    noPaymentsLabel.setPadding(new Insets(10));
                    paymentContainer.getChildren().add(noPaymentsLabel);
                } else {
                    for (Payment payment : pendingPayments) {
                        paymentContainer.getChildren().add(createPaymentRow(payment));
                        paymentContainer.getChildren().add(new Separator());
                    }
                }
                logger.info("Loaded {} pending students.", pendingPayments.size());
            });
        }).start();
    }

    private GridPane createPaymentRow(Payment payment) {

        logger.debug("Creating student row for student ID: {}", payment.getStudentNumber());
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(8, 10, 8, 10));

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPrefWidth(115);
        col0.setHalignment(HPos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(200);
        col1.setMinWidth(10);
        col1.setHgrow(Priority.SOMETIMES);
        col1.setHalignment(HPos.CENTER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(89);
        col2.setMinWidth(10);
        col2.setHgrow(Priority.SOMETIMES);
        col2.setHalignment(HPos.CENTER);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPrefWidth(89);
        col3.setMinWidth(10);
        col3.setHgrow(Priority.SOMETIMES);
        col3.setHalignment(HPos.CENTER);

        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPrefWidth(90);
        col4.setMinWidth(10);
        col4.setHgrow(Priority.SOMETIMES);
        col4.setHalignment(HPos.CENTER);

        ColumnConstraints col5 = new ColumnConstraints();
        col5.setPrefWidth(90);
        col5.setHalignment(HPos.CENTER);

        ColumnConstraints col6 = new ColumnConstraints();
        col6.setPrefWidth(92);
        col6.setHalignment(HPos.CENTER);

        gridPane.getColumnConstraints().addAll(col0, col1, col2, col3, col4, col5, col6);

        Label transactionIdLabel = new Label(payment.getTransactionId());
        transactionIdLabel.setFont(Font.font(13));

        Label studentNameLabel = new Label(payment.getFullName());
        studentNameLabel.setFont(Font.font(13));

        Label totalDueLabel = new Label("₱" + payment.getAssessment());
        totalDueLabel.setFont(Font.font(13));

        Label amountPaidLabel = new Label("₱" + payment.getAmount());
        amountPaidLabel.setFont(Font.font(13));

        Label eligibilityLabel = new Label(payment.getEligibility());
        eligibilityLabel.setFont(Font.font(13));

        Label paymentStatusLabel = new Label(payment.getStatus());
        paymentStatusLabel.setFont(Font.font(13));

        Button acceptButton = new Button("✓");
        acceptButton.getStyleClass().add("accept-button");
        acceptButton.setFont(Font.font("System Bold", 14));
        acceptButton.setOnAction(_ -> {
            int rowIndex = paymentContainer.getChildren().indexOf(gridPane);
            if (rowIndex != -1) {
                paymentContainer.getChildren().remove(gridPane);
                if (rowIndex < paymentContainer.getChildren().size() && paymentContainer.getChildren().get(rowIndex) instanceof Separator) {
                    paymentContainer.getChildren().remove(rowIndex);
                }
            } else {
                logger.warn("Could not find student payment row in UI list for optimistic removal: {}", payment.getPaymentId());
            }
            handlePaymentStatusUpdate(payment.getPaymentId(), "Approved");
        });

        Button rejectButton = new Button("✗");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setFont(Font.font("System Bold", 14));
        rejectButton.setOnAction(_ -> handlePaymentStatusUpdate(payment.getPaymentId(), "Rejected"));

        HBox actionsBox = new HBox(5, acceptButton, rejectButton);
        actionsBox.setAlignment(Pos.CENTER);

        gridPane.add(transactionIdLabel, 0, 0);
        gridPane.add(studentNameLabel, 1, 0);
        gridPane.add(totalDueLabel, 2, 0);
        gridPane.add(amountPaidLabel, 3, 0);
        gridPane.add(eligibilityLabel, 4, 0);
        gridPane.add(paymentStatusLabel, 5, 0);
        gridPane.add(actionsBox, 6, 0);

        logger.debug("Created student payment row for stundnt Number: {}", payment.getStudentNumber());
        return gridPane;
    }

    private void handlePaymentStatusUpdate(int paymentId, String newStatus) {

        logger.info("{} student payment: {}", newStatus, paymentId);
        new Thread(() -> {
            try {
                String sql = "UPDATE public.payments SET status = ?, approved_at = ? WHERE payment_id = ?";
                logger.debug("Preparing SQL update for payment ID {}: {}", paymentId, sql);

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, newStatus);
                    pstmt.setTimestamp(2, Timestamp.valueOf(LocalDate.now().atTime(LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                    pstmt.setInt(3, paymentId);

                    int affectedRows = pstmt.executeUpdate();
                    logger.debug("Update result: {} row(s) affected for payment ID {}", affectedRows, paymentId);

                    if (affectedRows > 0) {
                        logger.info("Payment ID {} marked as {}.", paymentId, newStatus);
                        Platform.runLater(() -> {
                            String alertTitle = "Payment " + newStatus;
                            String alertMessage = "The student payment has been " + newStatus.toLowerCase() + ".";
                            StageAndSceneUtils.showAlert(alertTitle, alertMessage, Alert.AlertType.INFORMATION);
                            loadPendingStudentPayments();
                        });
                    } else {
                        logger.warn("No rows affected while updating payment ID {} to {}.", paymentId, newStatus);
                        Platform.runLater(() -> {
                            String alertTitle = newStatus + " Failed";
                            String alertMessage = "Could not update the payment status. Please try again.";
                            StageAndSceneUtils.showAlert(alertTitle, alertMessage, Alert.AlertType.WARNING);
                        });
                    }

                }
            } catch (SQLException e) {
                logger.error("Database error while updating payment ID {} to {}: {}", paymentId, newStatus, e.getMessage(), e);
                Platform.runLater(() -> StageAndSceneUtils.showAlert("Database Error", "An error occurred while updating the payment status.", Alert.AlertType.ERROR));
            } catch (Exception e) {
                logger.error("Unexpected error while updating payment ID {} to {}: {}", paymentId, newStatus, e.getMessage(), e);
                Platform.runLater(() -> StageAndSceneUtils.showAlert("Error", "Something went wrong while updating the payment.", Alert.AlertType.ERROR));
            }
        }).start();
    }
}
