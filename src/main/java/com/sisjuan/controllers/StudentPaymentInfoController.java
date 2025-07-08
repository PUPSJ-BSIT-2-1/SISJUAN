
package com.sisjuan.controllers;

import com.sisjuan.utilities.DBConnection;
import com.sisjuan.utilities.SessionData;
import com.sisjuan.utilities.StageAndSceneUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StudentPaymentInfoController {

    // Student Information
    @FXML private Label studentID;
    @FXML private Label studentName;
    @FXML private Label studentProgram;
    @FXML private Label studentYearSection;
    @FXML private Button viewAccountsButton;


    // Payment Method
    @FXML private RadioButton cardRadio;
    @FXML private RadioButton bankRadio;
    @FXML private RadioButton gcashRadio;
    @FXML private RadioButton fheActRadio;

    // Payment Summary
    @FXML private HBox informationPaymentHBox;
    @FXML private TextField amountPaidField;
    @FXML private TextField totalFeesField;
    @FXML private Button submitPaymentButton;

    // Fee Breakdown Labels
    @FXML private Label academicFeeLabel;
    @FXML private Label academicFeeAmountLabel;
    @FXML private Label medicalFeeLabel;
    @FXML private Label libraryFeeLabel;
    @FXML private Label registrationFeeLabel;
    @FXML private Label developmentFeeLabel;
    @FXML private Label sisFeeLabel;
    @FXML private Label nstpFeeLabel;
    @FXML private Label totalAmountDueLabel;

    // Eligible for FHE Act
    @FXML private HBox eligibleCard;
    @FXML private HBox notEligibleCard;

    // Officially Enrolled
    @FXML private Label officiallyEnnrolledLabel;
    @FXML private Label notOfficiallyEnnrolledLabel;
    @FXML private Button viewScheduleButton;

    // Loading indicators
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox mainContent;

    // General Variables for Payment
    @FXML private BorderPane root;
    private double currentBalance = 0.00;
    private double initialBalance = 0.00;
    private double tuitionPerUnit = 0.00;
    private double totalFees = 0.00;
    private int numberOfUnitsEnrolled;
    private final List<Fee> fees = new ArrayList<>();
    protected double xOffset = 0;
    protected double yOffset = 0;

    Logger logger = LoggerFactory.getLogger(StudentPaymentInfoController.class.getName());

    private StudentDashboardController studentDashboardController;
    private StudentEnrollmentController enrollmentController;

    public void setStudentDashboardController(StudentDashboardController controller) {
        this.studentDashboardController = controller;
    }

    public void setEnrollmentController(StudentEnrollmentController controller) {
        this.enrollmentController = controller;
    }


        @FXML
    public void initialize() {

        logger.info("Initializing StudentPaymentInfoController...");

        // Initialize UI state
        setupInitialUIState();

        // Setup payment method radio buttons
        setupPaymentMethodControls();

        // Setup input validation
        setupInputValidation();

        // Setup event handlers
        setupEventHandlers();

        // Load data asynchronously
        loadDataAsync();

    }

    private void setupInitialUIState() {
        // Show loading state

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        if (mainContent != null) {
            mainContent.setDisable(true);
        }

        // Disable the submitted button initially
        submitPaymentButton.setDisable(true);

        // Set placeholder text for better UX
        amountPaidField.setPromptText("Enter payment amount");
        totalFeesField.setPromptText("Total fees will be calculated...");
        totalFeesField.setEditable(false);
    }

    private void setupPaymentMethodControls() {
        // Group the radio buttons
        ToggleGroup paymentGroup = new ToggleGroup();
        cardRadio.setToggleGroup(paymentGroup);
        bankRadio.setToggleGroup(paymentGroup);
        gcashRadio.setToggleGroup(paymentGroup);
        fheActRadio.setToggleGroup(paymentGroup);

        // Add a listener for payment method selection
        paymentGroup.selectedToggleProperty().addListener((_, _, _) -> validatePaymentForm());
    }

    private void setupInputValidation() {
        // Enhanced amount of field validation with real-time feedback

        amountPaidField.textProperty().addListener((_, oldValue, newValue) -> {
            // Allow only numbers, commas, and decimal points
            if (!newValue.matches("[\\d,.]*(\\.\\d{0,2})?")) {
                amountPaidField.setText(oldValue);
                return;
            }

            validatePaymentForm();
        });

        // Add focus listeners for better UX
        amountPaidField.focusedProperty().addListener((_, _, isNowFocused) -> {
            if (isNowFocused) {
                // Clear placeholder formatting when focused
                String text = amountPaidField.getText();
                if (text.startsWith("₱")) {
                    amountPaidField.setText(text.replace("₱", "").replace(",", "").trim());
                }
            } else if (!amountPaidField.getText().isEmpty()) {
                // Format currency when focus is lost
                formatAmountField();
            }
        });
    }

    private void setupEventHandlers() {

        viewAccountsButton.setOnAction(this::handleViewAccounts);
        viewScheduleButton.setOnAction(_ -> {
            handleViewSchedule();
            String SCHEDULE_FXML = "/com/sisjuan/fxml/StudentClassSchedule.fxml";
            studentDashboardController.handleQuickActionClicks(SCHEDULE_FXML);
        });
        informationPaymentHBox.setOnMouseClicked(_ -> showPaymentInformationAlert(root.getScene().getRoot()));
        submitPaymentButton.setOnAction(_ -> handleSubmitPaymentAsync());
    }

    public void loadDataAsync() {

        String studentNumber = SessionData.getInstance().getStudentNumber();

        Task<Void> loadDataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                // Simulate realistic loading time for better UX
                Thread.sleep(500);

                // Load data in the background thread
                loadStudentInfo(studentNumber);
                determineEligibility();
                getTotalEnrolledUnits();
                if (numberOfUnitsEnrolled > 0) {
                    loadFeeBreakdown();
                }
                return null;
            }

            @Override
            protected void succeeded() {

                Platform.runLater(() -> {
                    try {
                        setFeeBreakdown();
                        determineEnrollmentStatus();

                        // Hide loading state
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisible(false);
                        }
                        if (mainContent != null) {
                            mainContent.setDisable(false);
                        }

                        logger.info("StudentPaymentInfoController initialization completed successfully");
                    } catch (Exception e) {
                        logger.error("Error completing initialization", e);
                        handleLoadingError(e);
                    }
                });
            }

            @Override
            protected void failed() {

                Platform.runLater(() -> {
                    logger.error("Failed to load payment information", getException());
                    handleLoadingError(getException());
                });
            }
        };

        // Run task in background thread
        Thread loadThread = new Thread(loadDataTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void handleLoadingError(Throwable error) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);

        }
        if (mainContent != null) {
            mainContent.setDisable(false);
        }

        StageAndSceneUtils.showAlert(
                "Loading Error",
                "Failed to load payment information. Please try refreshing the page.\n\nError: " + error.getMessage(),
                Alert.AlertType.ERROR
        );
    }

    private void validatePaymentForm() {
        boolean isValid = true;
        String errorMessage = "";


        // Check if a payment method is selected
        ToggleGroup paymentGroup = cardRadio.getToggleGroup();
        if (paymentGroup.getSelectedToggle() == null) {
            isValid = false;
            errorMessage = "Please select a payment method";
        }

        // Check amount validity
        try {
            String amountText = amountPaidField.getText().replaceAll("[₱,\\s]", "");
            if (amountText.isEmpty()) {
                isValid = false;
                errorMessage = "Please enter payment amount";
            } else {
                double enteredAmount = Double.parseDouble(amountText);
                if (enteredAmount <= 0) {
                    isValid = false;
                    errorMessage = "Amount must be greater than zero";
                } else if (enteredAmount != totalFees && totalFees > 0) {
                    isValid = false;
                    errorMessage = String.format("Amount must equal total fees: ₱%,.2f", totalFees);
                }
            }
        } catch (NumberFormatException e) {
            isValid = false;
            errorMessage = "Invalid amount format";
        }

        // Update UI based on validation
        submitPaymentButton.setDisable(!isValid);

        // Show/hide error tooltip
        if (!isValid && !errorMessage.isEmpty()) {
            submitPaymentButton.setTooltip(new Tooltip(errorMessage));
        } else {
            submitPaymentButton.setTooltip(null);
        }
    }

    private void formatAmountField() {
        try {
            String text = amountPaidField.getText().replaceAll("[₱,\\s]", "");
            if (!text.isEmpty()) {

                double amount = Double.parseDouble(text);
                amountPaidField.setText(String.format("₱%,.2f", amount));
            }
        } catch (NumberFormatException e) {
            // Keep the original text if formatting fails
        }
    }

    private void determineEligibility() {
        String studentNumber = SessionData.getInstance().getStudentNumber();

        String query = """
            SELECT f.status_name
            FROM public.students s
            JOIN public.fhe_act_statuses f ON s.fhe_eligible_id = f.fhe_id
            WHERE s.student_number = ?
        """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String statusName = rs.getString("status_name"); // get before runLater
                    Platform.runLater(() -> {
                        if (Objects.equals(statusName, "Eligible")) {
                            eligibleCard.setOpacity(1);
                            notEligibleCard.setOpacity(0);
                            fheActRadio.setSelected(true);
                            fheActRadio.setDisable(false);
                        } else {
                            notEligibleCard.setOpacity(1);
                            eligibleCard.setOpacity(0);
                            fheActRadio.setDisable(true);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error determining eligibility", e);
        }
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

                    final String studentId = rs.getString("student_number");
                    final String fullName = rs.getString("full_name");
                    final String sectionName = rs.getString("section_name");

                    Platform.runLater(() -> {
                        studentID.setText(studentId);
                        studentName.setText(fullName);
                        splitStudentProgram(sectionName);
                    });
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

    private void loadFeeBreakdown() {

        String query = "SELECT tuition_name, price FROM fees";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            fees.clear(); // Clear existing fees
            while (rs.next()) {
                String feeName = rs.getString("tuition_name");
                double feePrice = rs.getDouble("price");

                Fee fee = new Fee(feeName, feePrice);
                fees.add(fee);
            }

        } catch (Exception e) {
            logger.error("Error loading fee breakdown: {}", e.getMessage());
        }
    }

    private void setFeeBreakdown() {

        totalFees = 0.00; // Reset total

        // Set existing fee breakdown labels
        for (Fee fee : fees) {
            switch (fee.name()) {
                case "Medical & Dental" -> medicalFeeLabel.setText(String.format("₱%,.2f", fee.price()));
                case "Library" -> libraryFeeLabel.setText(String.format("₱%,.2f", fee.price()));
                case "Registration" -> registrationFeeLabel.setText(String.format("₱%,.2f", fee.price()));
                case "Development" -> developmentFeeLabel.setText(String.format("₱%,.2f", fee.price()));
                case "SIS" -> sisFeeLabel.setText(String.format("₱%,.2f", fee.price()));
                case "NSTP" -> nstpFeeLabel.setText(String.format("₱%,.2f", fee.price()));
                case "Tuition (Per Unit)" -> tuitionPerUnit = fee.price();
            }
        }

        academicFeeLabel.setText(String.format("Academic Fee (%d Tuition Units)", numberOfUnitsEnrolled));
        academicFeeAmountLabel.setText(String.format("₱%,.2f", tuitionPerUnit * numberOfUnitsEnrolled));

        for (Fee fee : fees) {
            totalFees += fee.price();
        }

        totalFees += tuitionPerUnit * numberOfUnitsEnrolled;
        totalAmountDueLabel.setText(String.format("₱%,.2f", totalFees));
        totalFeesField.setText(String.format("₱%,.2f", totalFees));
        initialBalance = totalFees;
        currentBalance = totalFees;

        // Trigger validation after fees are loaded
        validatePaymentForm();
    }

    private void showPaymentInformationAlert(javafx.scene.Parent borderPane) {

        Dialog<Void> dialog = new Dialog<>();

        dialog.getDialogPane().setPrefSize(450, 300);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/sisjuan/css/GeneralCalendar.css")).toExternalForm());

        if (borderPane.getStyleClass().contains("dark-theme")) {
            dialog.getDialogPane().getScene().getRoot().getStyleClass().addAll("dark-custom-dialog", "dark-theme");
        }

        dialog.getDialogPane().getStyleClass().add("custom-dialog");
        VBox content = new VBox(15);

        Label title = new Label("💳 Payment Information");
        title.getStyleClass().add("custom-dialog-header");

        Separator separator = new Separator();
        separator.setOrientation(Orientation.HORIZONTAL);

        Label subtitle = new Label("📋 Important Payment Instructions:");
        subtitle.getStyleClass().add("custom-dialog-subheader");

        VBox instructionsBox = new VBox(8);
        List<String> instructions = Arrays.asList(
                "• Enter the exact total amount shown below.",
                "• Select your preferred payment method.",
                "• Click 'Submit Payment' to process.",
                "• Payment will be reviewed by administration"
        );

        for (String instruction : instructions) {
            Label instructionLabel = new Label(instruction);
            instructionLabel.getStyleClass().add("custom-dialog-description");
            instructionsBox.getChildren().add(instructionLabel);
        }
        instructionsBox.getStyleClass().add("custom-dialog-description");

        Label totalFeesLabel = new Label("💰 Total Amount: " + String.format("₱%,.2f", totalFees));
        totalFeesLabel.getStyleClass().add("custom-dialog-total-fees");
        totalFeesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        content.getChildren().addAll(title, subtitle, separator, instructionsBox, totalFeesLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.getDialogPane().getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Got it!");
        okButton.getStyleClass().add("custom-dialog-button");

        // Make dialog draggable
        dialog.getDialogPane().setOnMousePressed(event -> {
            xOffset = dialog.getDialogPane().getScene().getWindow().getX() - event.getScreenX();
            yOffset = dialog.getDialogPane().getScene().getWindow().getY() - event.getScreenY();
        });

        dialog.getDialogPane().setOnMouseDragged(event -> {
            dialog.getDialogPane().getScene().getWindow().setX(event.getScreenX() + xOffset);
            dialog.getDialogPane().getScene().getWindow().setY(event.getScreenY() + yOffset);
        });

        // Center dialog
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        double x = primScreenBounds.getMinX() + (primScreenBounds.getWidth() - dialog.getDialogPane().getPrefWidth()) / 2 + 40;
        double y = primScreenBounds.getMinY() + (primScreenBounds.getHeight() - dialog.getDialogPane().getPrefHeight()) / 2 - 30;
        dialog.getDialogPane().getScene().getWindow().setX(x);
        dialog.getDialogPane().getScene().getWindow().setY(y);

        dialog.showAndWait();
    }

    private void handleSubmitPaymentAsync() {

        String method = getSelectedPaymentMethod();
        if (method == null) {
            StageAndSceneUtils.showAlert("Payment Method Required",
                    "Please select a payment method before submitting.", Alert.AlertType.WARNING);
            return;
        }

        // Show confirmation dialog
        Alert confirmAlert = getConfirmAlert(method);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                submitPaymentToDatabase(method);
            }
        });
    }

    private Alert getConfirmAlert(String method) {

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Payment");
        confirmAlert.setHeaderText("Submit Payment Confirmation");
        confirmAlert.setContentText(String.format(
                """
                Are you sure you want to submit a payment of ₱%,.2f using %s?
                This action cannot be undone and the payment will be
                immediately reviewed by administration.""",
                totalFees, method
        ));

        DialogPane dialogPane = confirmAlert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE); // ensures it respects preferred height
        dialogPane.setPrefWidth(400); // set preferred width
        dialogPane.setPrefHeight(200); // set preferred height
        return confirmAlert;
    }

    private void submitPaymentToDatabase(String method) {

        // Disable UI during submission
        submitPaymentButton.setDisable(true);
        submitPaymentButton.setText("Processing...");

        Task<Boolean> submitTask = new Task<>() {

            @Override
            protected Boolean call() {
                try {
                    String amountStr = amountPaidField.getText().replaceAll("[₱,\\s]", "");
                    double amountPaid = Double.parseDouble(amountStr);

                    if (amountPaid <= 0 || amountPaid != totalFees) {
                        throw new IllegalArgumentException("Invalid payment amount");
                    }

                    currentBalance -= amountPaid;
                    return insertPaymentToDatabase(amountPaid, method);

                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid amount format");
                }
            }


            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    submitPaymentButton.setText("Submit Payment");

                    if (getValue()) {
                        StageAndSceneUtils.showAlert("Payment Submitted Successfully",
                                """
                                        Your payment has been submitted and is now under review. 
                                        You will be notified once the payment is processed.""",
                                Alert.AlertType.INFORMATION);
                        resetPaymentFields();
                    } else {
                        submitPaymentButton.setDisable(false);
                        StageAndSceneUtils.showAlert("Payment Submission Failed",
                                "There was an error submitting your payment. Please try again.",
                                Alert.AlertType.ERROR);
                    }
                });
            }

            @Override
            protected void failed() {

                Platform.runLater(() -> {
                    submitPaymentButton.setText("Submit Payment");
                    submitPaymentButton.setDisable(false);

                    String errorMsg = getException().getMessage();
                    StageAndSceneUtils.showAlert("Payment Error",
                            "Payment submission failed: " + errorMsg, Alert.AlertType.ERROR);
                });
            }
        };

        Thread submitThread = new Thread(submitTask);
        submitThread.setDaemon(true);
        submitThread.start();
    }

    private boolean insertPaymentToDatabase(double amountPaid, String method) {
        String studentNumber = SessionData.getInstance().getStudentNumber();

        String query = """
                    INSERT INTO payments (transaction_id, student_number, assessment, balance, amount, payment_source, created_at, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
            pstmt.setString(2, studentNumber);
            pstmt.setDouble(3, initialBalance);
            pstmt.setDouble(4, currentBalance);
            pstmt.setDouble(5, amountPaid);
            pstmt.setString(6, method);
            pstmt.setTimestamp(7, Timestamp.valueOf(LocalDate.now().atTime(LocalTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            pstmt.setString(8, "Pending");

            int result = pstmt.executeUpdate();
            return result > 0;
        } catch (Exception e) {
            logger.error("Error inserting payment to database: {}", e.getMessage());
            return false;
        }
    }

    private void resetPaymentFields() {

        amountPaidField.setText("");
        amountPaidField.setPromptText("Enter payment amount");

        // Clear radio button selection
        ToggleGroup paymentGroup = cardRadio.getToggleGroup();
        paymentGroup.selectToggle(null);

        submitPaymentButton.setDisable(true);

        // Refresh enrollment status
        determineEnrollmentStatus();
    }

    private String getSelectedPaymentMethod() {

        if (cardRadio.isSelected()) return "Credit/Debit Card";
        if (bankRadio.isSelected()) return "Bank Transfer";
        if (gcashRadio.isSelected()) return "GCash";
        if (fheActRadio.isSelected()) return "FHE Act (Government)";
        return null;
    }

    private void determineEnrollmentStatus() {

        String studentNumber = SessionData.getInstance().getStudentNumber();

        Task<String> statusTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT status FROM payments WHERE student_number = ? ORDER BY created_at DESC LIMIT 1")) {
                    pstmt.setString(1, studentNumber);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("status");
                        }
                    }
                }
                return null;
            }


            @Override
            protected void succeeded() {

                Platform.runLater(() -> {
                    String status = getValue();
                    if (status != null && status.equals("Approved")) {
                        officiallyEnnrolledLabel.setOpacity(1);
                        notOfficiallyEnnrolledLabel.setOpacity(0);

                        // Disable payment form for enrolled students
                        amountPaidField.setDisable(true);
                        submitPaymentButton.setDisable(true);
                        cardRadio.setDisable(true);
                        bankRadio.setDisable(true);
                        gcashRadio.setDisable(true);
                        fheActRadio.setDisable(true);
                        resetPaymentFields();
                    } else {
                        notOfficiallyEnnrolledLabel.setOpacity(1);
                        officiallyEnnrolledLabel.setOpacity(0);
                    }
                });
            }

            @Override
            protected void failed() {
                logger.error("Error determining enrollment statusError determining enrollment status", getException());
            }
        };

        Thread statusThread = new Thread(statusTask);
        statusThread.setDaemon(true);
        statusThread.start();
    }

    @FXML
    private void handleViewAccounts(ActionEvent event) {

        try {
            ScrollPane contentPane = (ScrollPane) root.getScene().lookup("#contentPane");

            if (contentPane != null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/sisjuan/fxml/StudentPaymentHistory.fxml")
                );

                Parent newContent = loader.load();
                StudentPaymentHistoryController controller = loader.getController();

                contentPane.setContent(newContent);

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
        } catch (IOException e) {
            logger.error("Error loading StudentPaymentHistory.fxml: {}", e.getMessage());
            StageAndSceneUtils.showAlert("Navigation Error",
                    "Unable to load payment history. Please try again.", Alert.AlertType.ERROR);
        }
    }

    private void handleViewSchedule() {
        try {
            ScrollPane contentPane = (ScrollPane) root.getScene().lookup("#contentPane");

            if (contentPane != null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/sisjuan/fxml/StudentClassSchedule.fxml")
                );

                Parent newContent = loader.load();
                StudentClassScheduleController controller = loader.getController();

                contentPane.setContent(newContent);

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
        } catch (IOException e) {
            logger.error("Error loading StudentClassSchedule.fxml: {}", e.getMessage());
            StageAndSceneUtils.showAlert("Navigation Error",
                    "Unable to load class schedule. Please try again.", Alert.AlertType.ERROR);
        }
    }

    public void getTotalEnrolledUnits() {
        String studentNumber = SessionData.getInstance().getStudentNumber();

        String sql = "SELECT SUM(CAST(s.units AS INTEGER)) AS total_units " +
                "FROM student_load e " +
                "JOIN subjects s ON e.subject_id = s.subject_id " +
                "WHERE e.student_pk_id = (SELECT student_id FROM students WHERE student_number = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                numberOfUnitsEnrolled = rs.getInt("total_units");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private record Fee(String name, double price) {}
}