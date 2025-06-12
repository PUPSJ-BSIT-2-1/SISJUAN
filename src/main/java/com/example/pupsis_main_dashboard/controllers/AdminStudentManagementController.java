package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection; 
import com.example.pupsis_main_dashboard.utilities.EmailService;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester; 
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.MessagingException;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;

import static com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils.showAlert;

public class AdminStudentManagementController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AdminStudentManagementController.class);
    private final EmailService emailService = new EmailService();

    @FXML
    private VBox studentListContainer; 
    private VBox studentList; 
    private CheckBox selectAllCheckBox;
    @FXML
    private Button batchAcceptButton;
    private List<StudentData> currentDisplayedStudents = new ArrayList<>(); 

    @FXML private DatePicker firstSemStartDatePicker;
    @FXML private DatePicker secondSemStartDatePicker;
    @FXML private Button confirmFirstSemButton;
    @FXML private Button confirmSecondSemButton;
    @FXML private DatePicker firstSemEndDatePicker;
    @FXML private DatePicker secondSemEndDatePicker;
    @FXML private Button confirmFirstSemEndButton;
    @FXML private Button confirmSecondSemEndButton;

    private static class StudentData {
        int id;
        String firstName;
        String lastName;
        String status;
        String section;
        CheckBox checkBox; 

        public StudentData(int id, String firstName, String lastName, String status, String section) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.status = status;
            this.section = section;
            this.checkBox = new CheckBox();
        }

        public String getFullName() {
            return lastName + ", " + firstName;
        }

        public boolean isSelected() {
            return checkBox.isSelected();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing AdminStudentManagementController...");

        // Setup header controls for batch operations
        selectAllCheckBox = new CheckBox("Select All");
        selectAllCheckBox.getStyleClass().add("student-check-box");
        selectAllCheckBox.setOnAction(event -> {
            boolean select = selectAllCheckBox.isSelected();
            for (StudentData student : currentDisplayedStudents) {
                student.checkBox.setSelected(select);
            }
        });

        batchAcceptButton.getStyleClass().add("batch-accept-button");
        batchAcceptButton.setOnAction(event -> handleBatchAcceptSelected());

        if (confirmFirstSemButton != null) {
            confirmFirstSemButton.setOnAction(event -> handleConfirmSemesterStartDate());
        }
        if (confirmSecondSemButton != null) {
            confirmSecondSemButton.setOnAction(event -> handleConfirmSemesterStartDate());
        }
        if (confirmFirstSemEndButton != null) {
            confirmFirstSemEndButton.setOnAction(event -> handleConfirmSemesterEndDate());
        }
        if (confirmSecondSemEndButton != null) {
            confirmSecondSemEndButton.setOnAction(event -> handleConfirmSemesterEndDate());
        }

        HBox headerControls = new HBox(10, selectAllCheckBox, batchAcceptButton);
        headerControls.setPadding(new Insets(5, 0, 10, 0));
        headerControls.setAlignment(Pos.CENTER_LEFT);

        // This is the VBox that will hold the actual student rows
        studentList = new VBox(); // Initialize the class member VBox for student rows

        if (studentListContainer != null) {
            studentListContainer.getChildren().clear(); // Clear the FXML container
            studentListContainer.getChildren().addAll(headerControls, studentList); // Add header and the studentList VBox directly
        } else {
            // This is a critical FXML loading/injection issue if this happens.
            logger.error("CRITICAL FXML ERROR: studentListContainer is null. " +
                         "Check AdminStudentManagement.fxml for a VBox with fx:id=\"studentListContainer\". " +
                         "UI will not load correctly.");
            // As a fallback, to prevent crashes if other parts of the code expect studentList to be non-null,
            // ensure it's initialized, though it won't be visible.
            if (this.studentList == null) { // Should have been initialized above, but as a safeguard.
                this.studentList = new VBox();
            }
        }

        loadPendingStudents();
        autoAdvanceEligibleStudentsIfNeeded();
        logger.info("AdminStudentManagementController initialized.");
    }

    private void loadPendingStudents() {
        logger.info("Loading pending students...");
        currentDisplayedStudents.clear(); 
        new Thread(() -> {
            List<StudentData> pendingStudentsList = new ArrayList<>();
            String sql = """
                SELECT s.student_id, s.firstname, s.lastname, ss.status_name AS status
                FROM public.students s
                JOIN public.student_statuses ss ON s.student_status_id = ss.student_status_id
                WHERE ss.status_name = 'Pending'
                ORDER BY s.lastname, s.firstname
            """;

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                logger.debug("Executing query to fetch pending students: {}", sql);
                while (rs.next()) {
                    StudentData student = new StudentData(
                            rs.getInt("student_id"),
                            rs.getString("firstname"),
                            rs.getString("lastname"),
                            rs.getString("status"),
                            "Pending Assignment" // Default for pending students
                    );
                    pendingStudentsList.add(student);
                    currentDisplayedStudents.add(student); 
                    logger.debug("Loaded pending student: {}", rs.getInt("student_id"));
                }
            } catch (SQLException e) {
                logger.error("SQL Error loading pending students: {}", e.getMessage(), e);
                Platform.runLater(() -> showAlert("Database Error", "Failed to load pending students."));
            }

            Platform.runLater(() -> {
                studentList.getChildren().clear(); 
                if (pendingStudentsList.isEmpty()) {
                    selectAllCheckBox.setSelected(false);
                    selectAllCheckBox.setDisable(true);
                    batchAcceptButton.setDisable(true);
                    Label noStudentsLabel = new Label("No pending student registrations found.");
                    noStudentsLabel.setPadding(new Insets(10));
                    studentList.getChildren().add(noStudentsLabel);
                } else {
                    selectAllCheckBox.setDisable(false);
                    batchAcceptButton.setDisable(false);
                    for (StudentData student : pendingStudentsList) {
                        studentList.getChildren().add(createStudentRow(student));
                        studentList.getChildren().add(new Separator()); 
                    }
                }
                logger.info("Loaded {} pending students.", pendingStudentsList.size());
            });
        }).start();
    }

    private GridPane createStudentRow(StudentData student) {
        logger.debug("Creating student row for student ID: {}", student.id);
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10); 
        gridPane.setPadding(new Insets(8, 10, 8, 10)); 

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPrefWidth(30);
        col0.setHalignment(HPos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(170); 
        col1.setMinWidth(10); 
        col1.setHgrow(Priority.SOMETIMES);
        col1.setHalignment(HPos.CENTER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(100);
        col2.setMinWidth(10);
        col2.setHgrow(Priority.SOMETIMES);
        col2.setHalignment(HPos.CENTER);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPrefWidth(120);
        col3.setMinWidth(10);
        col3.setHgrow(Priority.SOMETIMES);

        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPrefWidth(100);
        col4.setMinWidth(10);
        col4.setHgrow(Priority.SOMETIMES);
        col4.setHalignment(HPos.CENTER); 

        gridPane.getColumnConstraints().addAll(col0, col1, col2, col3, col4);

        student.checkBox.getStyleClass().add("student-check-box");
        gridPane.add(student.checkBox, 0, 0);

        Label nameLabel = new Label(student.getFullName());
        nameLabel.setFont(Font.font(13));

        Label statusLabel = new Label(student.status);
        statusLabel.setFont(Font.font(13));

        Label sectionLabel = new Label(student.section != null ? student.section : "N/A");
        sectionLabel.setFont(Font.font(13));

        Button acceptButton = new Button("✓");
        acceptButton.getStyleClass().add("accept-button");
        acceptButton.setFont(Font.font("System Bold", 14));
        acceptButton.setOnAction(_ -> {
            int rowIndex = studentList.getChildren().indexOf(gridPane);
            if (rowIndex != -1) {
                studentList.getChildren().remove(gridPane); 
                if (rowIndex < studentList.getChildren().size() && studentList.getChildren().get(rowIndex) instanceof Separator) {
                    studentList.getChildren().remove(rowIndex); 
                }
            } else {
                logger.warn("Could not find student row in UI list for optimistic removal: {}", student.id);
            }
            handleAcceptStudent(student.id);
        });

        Button rejectButton = new Button("✗");
        rejectButton.getStyleClass().add("reject-button");
        rejectButton.setFont(Font.font("System Bold", 14));
        rejectButton.setOnAction(_ -> handleRejectStudent(student.id));
        
        HBox actionsBox = new HBox(5, acceptButton, rejectButton);
        actionsBox.setAlignment(Pos.CENTER);

        gridPane.add(nameLabel, 1, 0); 
        gridPane.add(statusLabel, 2, 0); 
        gridPane.add(sectionLabel, 3, 0); 
        gridPane.add(actionsBox, 4, 0);   

        logger.debug("Created student row for student ID: {}", student.id);
        return gridPane;
    }

    @FXML
    private void handleBatchAcceptSelected() {
        List<StudentData> selectedStudents = new ArrayList<>();
        for (StudentData student : currentDisplayedStudents) {
            if (student.isSelected()) {
                selectedStudents.add(student);
            }
        }

        if (selectedStudents.isEmpty()) {
            showAlert("No Students Selected", "Please select students to batch accept.");
            return;
        }

        List<Node> nodesToRemove = new ArrayList<>();
        for (StudentData student : selectedStudents) {
            for (int i = 0; i < studentList.getChildren().size(); i++) {
                Node node = studentList.getChildren().get(i);
                if (node instanceof GridPane) {
                    GridPane rowPane = (GridPane) node;
                    if (rowPane.getChildren().contains(student.checkBox)) { 
                        nodesToRemove.add(rowPane);
                        if (i + 1 < studentList.getChildren().size() && studentList.getChildren().get(i+1) instanceof Separator) {
                            nodesToRemove.add(studentList.getChildren().get(i+1));
                        }
                        break; 
                    }
                }
            }
        }
        studentList.getChildren().removeAll(nodesToRemove);
        currentDisplayedStudents.removeAll(selectedStudents);
        if (currentDisplayedStudents.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setDisable(true);
            batchAcceptButton.setDisable(true);
        }

        logger.info("Batch accept initiated for {} students.", selectedStudents.size());
        new Thread(() -> {
            List<String> acceptanceResults = new ArrayList<>();
            int successCount = 0;

            for (StudentData student : selectedStudents) {
                String result = null;
                try {
                    result = processSingleStudentAcceptance(student.id);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                acceptanceResults.add(student.getFullName() + ": " + result);
                if (result.startsWith("Successfully")) {
                    successCount++;
                }
            }

            final int finalSuccessCount = successCount;
            Platform.runLater(() -> {
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("Batch Acceptance Report");
                dialog.setHeaderText("Processed " + selectedStudents.size() + " students. " + finalSuccessCount + " accepted.");

                VBox dialogContent = new VBox(10);
                dialogContent.setPadding(new Insets(10));
                ListView<String> resultListView = new ListView<>();
                resultListView.getItems().addAll(acceptanceResults);
                resultListView.setPrefHeight(200); 
                
                ScrollPane resultScrollPane = new ScrollPane(resultListView);
                resultScrollPane.setFitToWidth(true);
                resultScrollPane.setPrefHeight(200);
                VBox.setVgrow(resultScrollPane, Priority.ALWAYS);

                dialogContent.getChildren().add(resultScrollPane);
                dialog.getDialogPane().setContent(dialogContent);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
                dialog.showAndWait();

                loadPendingStudents(); 
            });
        }).start();
    }

    private String processSingleStudentAcceptance(int studentId) throws SQLException {
        Connection conn = null;
        try { // Outer try for connection and initial setup
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Fetch student details for email
            String studentFirstName = null;
            String studentLastName = null;
            String studentEmail = null;
            String getStudentDetailsSql = "SELECT firstname, lastname, email FROM public.students WHERE student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(getStudentDetailsSql)) {
                pstmt.setInt(1, studentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        studentFirstName = rs.getString("firstname");
                        studentLastName = rs.getString("lastname");
                        studentEmail = rs.getString("email");
                    } else {
                        conn.rollback();
                        logger.warn("Could not find student with ID {} to accept.", studentId);
                        return "Failed: Student not found.";
                    }
                }
            }

            int currentAcademicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();
            int currentSemesterId = SchoolYearAndSemester.getCurrentSemesterId();
            int yearLevelForStudent = 1;

            if (currentAcademicYearId == -1 || currentSemesterId == -1) {
                // This return will bypass the finally, but conn is null or will be handled by finally if an exception occurs before this.
                return "Failed: Could not determine current academic year/semester.";
            }

            String findSectionSql = """
                SELECT sec.section_id, sec.section_name, COUNT(stud_sec.student_id) AS current_students
                FROM public.section sec
                LEFT JOIN public.student_sections stud_sec ON sec.section_id = stud_sec.section_id
                WHERE sec.academic_year_id = ? AND sec.semester_id = ? AND sec.year_level = ?
                GROUP BY sec.section_id, sec.section_name, sec.max_capacity
                HAVING COUNT(stud_sec.student_id) < sec.max_capacity
                ORDER BY current_students ASC
                LIMIT 1
            """;
            String insertStudentSectionSql = "INSERT INTO public.student_sections (student_id, section_id) VALUES (?, ?)";
            String updateStudentSql = """
                UPDATE public.students 
                SET student_status_id = (
                        SELECT student_status_id 
                        FROM public.student_statuses 
                        WHERE status_name = 'Enrolled'
                    ), 
                    scholastic_status_id = (
                        SELECT scholastic_status_id 
                        FROM public.scholastic_statuses 
                        WHERE status_name = 'Regular'
                    ), 
                    fhe_eligible_id = (
                        SELECT fhe_id 
                        FROM public.fhe_act_statuses 
                        WHERE status_name = 'Eligible'
                    ), 
                    current_year_section_id = ? 
                WHERE student_id = ?
                """;

            try (PreparedStatement pstmtFindSection = conn.prepareStatement(findSectionSql);
                 PreparedStatement pstmtInsertStudentSection = conn.prepareStatement(insertStudentSectionSql);
                 PreparedStatement pstmtUpdateStudent = conn.prepareStatement(updateStudentSql)) {

                pstmtFindSection.setInt(1, currentAcademicYearId);
                pstmtFindSection.setInt(2, currentSemesterId);
                pstmtFindSection.setInt(3, yearLevelForStudent);

                logger.debug("Executing findSectionSql with params: AY_ID={}, SEM_ID={}, YEAR_LEVEL=1", currentAcademicYearId, currentSemesterId);
                try (ResultSet rs = pstmtFindSection.executeQuery()) {
                    if (rs.next()) {
                        int sectionId = rs.getInt("section_id");
                        String sectionName = rs.getString("section_name");

                        pstmtInsertStudentSection.setInt(1, studentId);
                        pstmtInsertStudentSection.setInt(2, sectionId);
                        logger.debug("Executing insertStudentSectionSql with params: StudentID={}, SectionID={}", studentId, sectionId);
                        pstmtInsertStudentSection.executeUpdate();

                        logger.debug("Executing updateStudentSql with params: SectionID={}, StudentID={}", sectionId, studentId);
                        pstmtUpdateStudent.setInt(1, sectionId);
                        pstmtUpdateStudent.setInt(2, studentId);
                        pstmtUpdateStudent.executeUpdate();

                        conn.commit();
                        logger.info("Successfully committed changes for student ID {}", studentId);

                        // Send acceptance email
                        if (studentEmail != null && !studentEmail.trim().isEmpty()) {
                            try {
                                emailService.sendAcceptanceEmail(studentEmail, studentFirstName + " " + studentLastName, sectionName);
                                logger.info("Acceptance email sent successfully to {}", studentEmail);
                            } catch (MessagingException e) {
                                logger.error("Failed to send acceptance email for student ID {}: {}", studentId, e.getMessage(), e);
                            }
                        } else {
                            logger.warn("Could not send acceptance email for student ID {}: email address is missing or empty.", studentId);
                        }

                        return "Successfully assigned to " + sectionName;
                    } else {
                        conn.rollback();
                        return "Failed: No suitable section found (Year Level: " + yearLevelForStudent + ").";
                    }
                }
            } catch (SQLException e) { // Catches for the inner try-with-resources
                logger.error("SQL Error during student processing (student ID {}): {}", studentId, e.getMessage());
                if (conn != null) {
                    conn.rollback(); // Rollback on inner SQL error
                }
                // Re-throw to be caught by the outer catch block
                throw e;
            }
        } catch (SQLException e) { // Catches connection errors or re-thrown errors
            logger.error("A database error occurred processing student ID {}: {}", studentId, e.getMessage(), e);
            // The connection might be null or closed, so rollback is handled in the inner catch.
            // This method is called from a background thread that handles UI updates,
            // so returning an error string is appropriate.
            return "Failed: A database error occurred.";
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit state
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Failed to close database connection for student ID {}: {}", studentId, e.getMessage(), e);
                }
            }
        }
    }

    private void handleAcceptStudent(int studentId) {
        logger.info("Accepting student ID: {}", studentId);
        new Thread(() -> {
            Connection conn = null;
            try { // Outer try for the lambda's operations
                conn = DBConnection.getConnection();
                conn.setAutoCommit(false); 

                // Fetch student details for email
                String studentFirstName = null;
                String studentLastName = null;
                String studentEmail = null;
                String getStudentDetailsSql = "SELECT firstname, lastname, email FROM public.students WHERE student_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(getStudentDetailsSql)) {
                    pstmt.setInt(1, studentId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            studentFirstName = rs.getString("firstname");
                            studentLastName = rs.getString("lastname");
                            studentEmail = rs.getString("email");
                        } else {
                            conn.rollback();
                            logger.warn("Could not find student with ID {} to accept.", studentId);
                            Platform.runLater(() -> showAlert("Failed", "Student not found."));
                            // No explicit rollback here as commit hasn't happened; finally will handle close.
                            return; // Exit thread execution
                        }
                    }
                }

                int currentAcademicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();
                int currentSemesterId = SchoolYearAndSemester.getCurrentSemesterId();
                int yearLevelForStudent = 1; 

                if (currentAcademicYearId == -1 || currentSemesterId == -1) {
                    logger.error("Could not determine current academic year/semester. Cannot accept student ID: {}", studentId);
                    Platform.runLater(() -> showAlert("System Error", "Could not determine current academic year or semester."));
                    // No explicit rollback here as commit hasn't happened; finally will handle close.
                    return; // Exit thread execution
                }

                String findSectionSql = """
                    SELECT sec.section_id, sec.section_name, COUNT(stud_sec.student_id) AS current_students
                    FROM public.section sec
                    LEFT JOIN public.student_sections stud_sec ON sec.section_id = stud_sec.section_id
                    WHERE sec.academic_year_id = ? AND sec.semester_id = ? AND sec.year_level = ?
                    GROUP BY sec.section_id, sec.section_name, sec.max_capacity
                    HAVING COUNT(stud_sec.student_id) < sec.max_capacity
                    ORDER BY current_students ASC
                    LIMIT 1
                """;
                String insertStudentSectionSql = "INSERT INTO public.student_sections (student_id, section_id) VALUES (?, ?)";
                String updateStudentSql = "UPDATE public.students SET student_status_id = (SELECT student_status_id FROM public.student_statuses WHERE status_name = 'Enrolled'), current_year_section_id = ? WHERE student_id = ?";

                try (PreparedStatement pstmtFindSection = conn.prepareStatement(findSectionSql);
                     PreparedStatement pstmtInsertStudentSection = conn.prepareStatement(insertStudentSectionSql);
                     PreparedStatement pstmtUpdateStudent = conn.prepareStatement(updateStudentSql)) {

                    pstmtFindSection.setInt(1, currentAcademicYearId);
                    pstmtFindSection.setInt(2, currentSemesterId);
                    pstmtFindSection.setInt(3, yearLevelForStudent);

                    logger.debug("Executing findSectionSql with params: AY_ID={}, SEM_ID={}, YEAR_LEVEL=1", currentAcademicYearId, currentSemesterId);
                    try (ResultSet rs = pstmtFindSection.executeQuery()) {
                        if (rs.next()) {
                            int sectionId = rs.getInt("section_id");
                            String sectionName = rs.getString("section_name");

                            pstmtInsertStudentSection.setInt(1, studentId);
                            pstmtInsertStudentSection.setInt(2, sectionId);
                            logger.debug("Executing insertStudentSectionSql with params: StudentID={}, SectionID={}", studentId, sectionId);
                            pstmtInsertStudentSection.executeUpdate();

                            logger.debug("Executing updateStudentSql with params: SectionID={}, StudentID={}", sectionId, studentId);
                            pstmtUpdateStudent.setInt(1, sectionId);
                            pstmtUpdateStudent.setInt(2, studentId);
                            pstmtUpdateStudent.executeUpdate();

                            conn.commit();
                            logger.info("Successfully committed changes for student ID {}", studentId);

                            // Send acceptance email
                            if (studentEmail != null && !studentEmail.trim().isEmpty()) {
                                try {
                                    emailService.sendAcceptanceEmail(studentEmail, studentFirstName + " " + studentLastName, sectionName);
                                    logger.info("Acceptance email sent successfully to {}", studentEmail);
                                } catch (MessagingException e) {
                                    logger.error("Failed to send acceptance email for student ID {}: {}", studentId, e.getMessage(), e);
                                }
                            } else {
                                logger.warn("Could not send acceptance email for student ID {}: email address is missing or empty.", studentId);
                            }

                            Platform.runLater(() -> {
                                showAlert("Success", "Student accepted and assigned to a section.");
                                loadPendingStudents(); 
                            });
                        } else {
                            logger.warn("No available sections found for student ID: {} in academic year: {}, semester: {}, year_level: 1", studentId, currentAcademicYearId, currentSemesterId);
                            Platform.runLater(() -> showAlert("No Section Available", "No suitable section found for the student in the current term and year level 1, or all sections are full."));
                            conn.rollback(); // Rollback if no section found
                        }
                    }
                } // Inner try-with-resources closes its resources (PreparedStatements, ResultSet)
                  // Exceptions from here will be caught by the outer catch blocks.

            } catch (SQLException e) { // Catch for the outer try
                logger.error("SQL Error accepting student ID {}: {}", studentId, e.getMessage(), e);
                if (conn != null) {
                    try {
                        conn.rollback(); 
                        logger.info("Transaction rolled back for student ID: {}", studentId);
                    } catch (SQLException ex) {
                        logger.error("Error rolling back transaction for student ID {}: {}", studentId, ex.getMessage(), ex);
                    }
                }
                Platform.runLater(() -> {
                    showAlert("Database Error", "Failed to accept student: " + e.getMessage());
                    loadPendingStudents(); 
                });
            } catch (Exception e) { // Catch for the outer try 
                 logger.error("Unexpected error accepting student ID {}: {}", studentId, e.getMessage(), e);
                 if (conn != null) { // Should have been rolled back by SQLException catch if that was the cause
                    try {
                        conn.rollback(); // Attempt rollback if not already done
                    } catch (SQLException ex) {
                        logger.error("Error rolling back transaction for student ID {}: {}", studentId, ex.getMessage(), ex);
                    }
                }
                Platform.runLater(() -> {
                    showAlert("System Error", "An unexpected error occurred: " + e.getMessage());
                    loadPendingStudents(); 
                });
            } finally { // Finally for the outer try (connection management)
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true); 
                    } catch (SQLException e) {
                        logger.error("Error setting autoCommit to true for conn: {}", e.getMessage(), e);
                    }
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        logger.error("Error closing connection: {}", e.getMessage(), e);
                    }
                }
            } // End of outer try's finally
        }).start();
    }

    private void handleRejectStudent(int studentId) {
        logger.info("Attempting to reject student ID: {}", studentId);
        new Thread(() -> {
            try {
                int rejectedStatusId = getStudentStatusIdByName("Rejected"); 
                String sql = "UPDATE public.students SET student_status_id = ? WHERE student_id = ?";
                logger.debug("Executing update status query for student ID {}: {}", studentId, sql);
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, rejectedStatusId);
                    pstmt.setInt(2, studentId);
                    int affectedRows = pstmt.executeUpdate();
                    logger.debug("Affected rows after status update: {}", affectedRows);
                    if (affectedRows > 0) {
                        logger.info("Student ID {} status updated to Rejected.", studentId);
                        Platform.runLater(() -> {
                            showAlert("Success", "Student rejected and status updated.");
                            loadPendingStudents(); 
                        });
                    } else {
                        logger.error("Failed to update student status to Rejected for student ID: {}. No rows affected.", studentId);
                        Platform.runLater(() -> showAlert("Error", "Failed to update student status."));
                    }
                }
            } catch (SQLException e) {
                logger.error("SQL Error during student rejection for student ID {}: {}", studentId, e.getMessage(), e);
                Platform.runLater(() -> showAlert("Database Error", "A database error occurred while rejecting the student."));
            } catch (Exception e) {
                logger.error("Unexpected error during student rejection for student ID {}: {}", studentId, e.getMessage(), e);
                Platform.runLater(() -> showAlert("Application Error", "An unexpected error occurred."));
            }
        }).start();
    }

    private int getStudentStatusIdByName(String statusName) throws SQLException {
        logger.debug("Getting student status ID for status name: {}", statusName);
        String query = "SELECT student_status_id FROM public.student_statuses WHERE status_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, statusName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int statusId = rs.getInt("student_status_id");
                    logger.debug("Found student status ID: {} for status name: {}", statusId, statusName);
                    return statusId;
                }
            }
        }
        logger.warn("Status ID not found for status name: {}", statusName);
        throw new SQLException("Status ID not found for name: " + statusName); 
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
        logger.debug("Showing alert: Title='{}', Content='{}'", title, content);
    }

    // === Advancement Logic ===
    private List<Integer> getAllStudentIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT student_id FROM public.students";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("student_id"));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all student IDs", e);
        }
        return ids;
    }

    // Returns true if student was advanced, false otherwise
    private boolean advanceStudentIfEligible(int studentId) {
        // 1. Fetch current context
        int sectionId = -1, yearLevel = -1, semesterId = -1, academicYearId = -1;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT s.current_year_section_id, sec.year_level, sec.semester_id, sec.academic_year_id " +
                "FROM public.students s JOIN public.section sec ON s.current_year_section_id = sec.section_id " +
                "WHERE s.student_id = ?")) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    sectionId = rs.getInt("current_year_section_id");
                    yearLevel = rs.getInt("year_level");
                    semesterId = rs.getInt("semester_id");
                    academicYearId = rs.getInt("academic_year_id");
                } else {
                    logger.warn("Student {} not found for advancement", studentId);
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching student context for advancement", e);
            return false;
        }
        // 2. Get required subjects for current section/year/semester
        Set<Integer> requiredSubjectIds = new HashSet<>();
        String requiredSubjectsSql = "SELECT s.subject_id FROM subjects s " +
                "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                "WHERE fl.section_id = ? AND fl.semester_id = ? AND s.year_level = ? AND s.semester_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(requiredSubjectsSql)) {
            stmt.setInt(1, sectionId);
            stmt.setInt(2, semesterId);
            stmt.setInt(3, yearLevel);
            stmt.setInt(4, semesterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requiredSubjectIds.add(rs.getInt("subject_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching required subjects for advancement", e);
            return false;
        }
        if (requiredSubjectIds.isEmpty()) {
            logger.warn("No required subjects found for student {} (section/year/semester)", studentId);
            return false;
        }
        // 3. Check passed grades
        Set<Integer> passedSubjectIds = new HashSet<>();
        String passedGradesSql = "SELECT subject_id FROM grade WHERE student_pk_id = ? AND subject_id = ANY (?) AND grade_status_id = 2";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(passedGradesSql)) {
            stmt.setInt(1, studentId);
            Array subjectArray = conn.createArrayOf("INTEGER", requiredSubjectIds.toArray());
            stmt.setArray(2, subjectArray);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    passedSubjectIds.add(rs.getInt("subject_id"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching passed grades for advancement", e);
            return false;
        }
        if (!passedSubjectIds.containsAll(requiredSubjectIds)) {
            logger.info("Student {} not eligible for advancement: not all required subjects passed", studentId);
            return false;
        }
        // 4. Find next section
        int nextYearLevel = yearLevel;
        int nextSemesterId = semesterId + 1;
        if (nextSemesterId > 3) { // 1=1st, 2=Summer, 3=2nd
            nextSemesterId = 1;
            nextYearLevel++;
        }
        int nextSectionId = -1;
        String nextSectionSql = "SELECT section_id FROM section WHERE year_level = ? AND semester_id = ? AND academic_year_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(nextSectionSql)) {
            stmt.setInt(1, nextYearLevel);
            stmt.setInt(2, nextSemesterId);
            stmt.setInt(3, academicYearId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nextSectionId = rs.getInt("section_id");
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding next section for advancement", e);
            return false;
        }
        if (nextSectionId == -1) {
            logger.warn("No next section found for advancement for student {}. Advancement halted.", studentId);
            return false;
        }
        // 5. Update student record
        String updateStudentSql = "UPDATE students SET current_year_section_id = ? WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateStudentSql)) {
            stmt.setInt(1, nextSectionId);
            stmt.setInt(2, studentId);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Student {} advanced to section {}", studentId, nextSectionId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error updating student section for advancement", e);
        }
        return false;
    }

    @FXML
    private void handleConfirmSemesterStartDate() {
        LocalDate firstStart = firstSemStartDatePicker.getValue();
        LocalDate firstEnd = firstSemEndDatePicker.getValue();
        LocalDate secondStart = secondSemStartDatePicker.getValue();
        LocalDate secondEnd = secondSemEndDatePicker.getValue();
        new Thread(() -> {
            try (Connection conn = DBConnection.getConnection()) {
                // Save 1st Semester (semester_id = 1)
                if (firstStart != null || firstEnd != null) {
                    String sql = "UPDATE semesters SET start_date = ?, end_date = ? WHERE semester_id = 1";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setDate(1, firstStart != null ? Date.valueOf(firstStart) : null);
                        stmt.setDate(2, firstEnd != null ? Date.valueOf(firstEnd) : null);
                        stmt.executeUpdate();
                    }
                }
                // Save 2nd Semester (semester_id = 3)
                if (secondStart != null || secondEnd != null) {
                    String sql = "UPDATE semesters SET start_date = ?, end_date = ? WHERE semester_id = 3";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setDate(1, secondStart != null ? Date.valueOf(secondStart) : null);
                        stmt.setDate(2, secondEnd != null ? Date.valueOf(secondEnd) : null);
                        stmt.executeUpdate();
                    }
                }
                Platform.runLater(() -> showAlert("Confirm Semester Dates", "Semester dates have been saved."));
            } catch (SQLException e) {
                logger.error("Error saving semester dates", e);
                Platform.runLater(() -> showAlert("Error", "Failed to save semester dates."));
            }
        }).start();
    }

    @FXML
    private void handleConfirmSemesterEndDate() {
        handleConfirmSemesterStartDate(); // Use the same logic for saving all dates
    }

    public void autoAdvanceEligibleStudentsIfNeeded() {
        // 1. Fetch semester start and end dates from DB
        // 2. Check if current date matches the logic for advancing
        // 3. If so, run the advancement logic (previously in handleAdvanceAllEligible)
        new Thread(() -> {
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "SELECT semester_id, start_date, end_date FROM semesters WHERE semester_id IN (1, 3)";
                Map<Integer, Date[]> semesterDates = new HashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int semId = rs.getInt("semester_id");
                        Date start = rs.getDate("start_date");
                        Date end = rs.getDate("end_date");
                        semesterDates.put(semId, new Date[]{start, end});
                    }
                }
                LocalDate now = LocalDate.now();
                boolean shouldAdvance = false;
                // Example logic: advance on the day after 1st sem end or 2nd sem end
                for (Map.Entry<Integer, Date[]> entry : semesterDates.entrySet()) {
                    Date end = entry.getValue()[1];
                    if (end != null && now.isEqual(end.toLocalDate().plusDays(1))) {
                        shouldAdvance = true;
                        break;
                    }
                }
                if (shouldAdvance) {
                    Platform.runLater(() -> advanceAllEligibleStudents());
                }
            } catch (SQLException e) {
                logger.error("Error checking semester dates for auto-advance", e);
            }
        }).start();
    }

    private void advanceAllEligibleStudents() {
        // Place the batch advancement logic here (from previous handleAdvanceAllEligible)
        List<Integer> studentIds = getAllStudentIds();
        int advancedCount = 0;
        for (Integer studentId : studentIds) {
            if (advanceStudentIfEligible(studentId)) {
                advancedCount++;
            }
        }
        // After advancing, clear semester dates in DB
        new Thread(() -> {
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "UPDATE semesters SET start_date = NULL, end_date = NULL WHERE semester_id IN (1, 3)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Error clearing semester dates after advancement", e);
            }
        }).start();
        showAlert("Auto-Advancement", "Eligible students have been advanced based on semester dates. " + advancedCount + " students advanced. Semester dates have been cleared.");
    }
}
