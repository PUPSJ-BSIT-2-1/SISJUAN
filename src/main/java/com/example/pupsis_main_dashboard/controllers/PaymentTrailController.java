package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.shape.SVGPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PaymentTrailController {

    // Student Information
    @FXML private Label studentID;
    @FXML private Label studentName;
    @FXML private Label studentProgram;
    @FXML private Label studentYearSection;

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
    @FXML private Label academicFeeLabel;
    @FXML private Label medicalFeeLabel;
    @FXML private Label libraryFeeLabel;
    @FXML private Label registrationFeeLabel;
    @FXML private Label developmentFeeLabel;
    @FXML private Label sisFeeLabel;
    @FXML private Label nstpFeeLabel;
    @FXML private Label totalAmountDueLabel;

    // Eligible for FHE Act
    @FXML private Label eligibleLabel;
    @FXML private SVGPath eligibleIcon;

    // Balance (optional)
    @FXML private Label balanceLabel;
    private double currentBalance = 0.00;
    private String studentNumber;

    Logger logger = LoggerFactory.getLogger(PaymentTrailController.class.getName());


    /*
     * mga gagawin ko sa controller
     * 1. you are not yet officially enrolled in the program
     * 2. you are not eligible to FHE Act
     * 3. set functionality for the payment breakdown
     * 4. ayusin database
     */


    @FXML
    public void initialize() {
        // Group the radio buttons
        ToggleGroup paymentGroup = new ToggleGroup();
        cardRadio.setToggleGroup(paymentGroup);
        bankRadio.setToggleGroup(paymentGroup);
        gcashRadio.setToggleGroup(paymentGroup);

        this.studentNumber = SessionData.getInstance().getStudentNumber();

        loadStudentInfo(studentNumber);

        // Placeholder for future setup of fee breakdown and total balance
        // (to be implemented with dynamic data from backend/database)

        updateBalanceLabel();

        // Handle payment submission
        submitPaymentButton.setOnAction(event -> handleSubmitPayment());
    }

    private void loadStudentInfo(String studentNumber) {
        String query = """
            SELECT s.student_number, s.firstname || ' ' || s.middlename || ' ' || s.lastname AS full_name, sc.section_name
            FROM students s
            JOIN section sc ON sc.section_id = s.current_year_section_id
            WHERE s.student_number = ?
        """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    studentID.setText(rs.getString("student_number"));
                    studentName.setText(rs.getString("full_name"));
                    splitStudentProgram(rs.getString("section_name"));
                }
            }
        } catch (Exception e) {
            logger.error("Error loading student information: {}", e.getMessage());
        }
    }

    private void splitStudentProgram(String studentSectionName) {
        String[] parts = studentSectionName.split(" ");
        String programName = parts[0];

        programName = switch (programName) {
            case "BSIT" -> "Bachelor of Science in Information Technology";
            case "BSCS" -> "Bachelor of Science in Computer Science";
            case "BSIS" -> "Bachelor of Science in Information Systems";
            case "BSCE" -> "Bachelor of Science in Computer Engineering";
            case "DIT" -> "Diploma in Information Technology";
            default -> "Unknown Program";
        };

        studentProgram.setText(programName);
        studentYearSection.setText(studentSectionName);
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