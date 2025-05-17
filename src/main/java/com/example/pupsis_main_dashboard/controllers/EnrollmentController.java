package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class EnrollmentController implements Initializable {

    @FXML
    private Button selectAllButton;
    
    @FXML
    private Button enrollButton;

    @FXML
    private VBox subjectListContainer;

    private List<CheckBox> subjectCheckboxes = new ArrayList<>();
    private Map<CheckBox, SubjectData> checkboxSubjectMap = new HashMap<>();
    private Map<CheckBox, ComboBox<String>> checkboxScheduleMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSubjects();
        // Set up the enrollment button click handler
        enrollButton.setOnAction(this::handleEnrollment);
    }
    
    @FXML
    private void handleSelectAll(ActionEvent event) {
        boolean allSelected = true;

        // Check if any checkbox is unchecked
        for (CheckBox checkbox : subjectCheckboxes) {
            if (!checkbox.isSelected()) {
                allSelected = false;
                break;
            }
        }

        // Toggle all checkboxes
        for (CheckBox checkbox : subjectCheckboxes) {
            checkbox.setSelected(!allSelected);
        }

        // Update button text based on state
        updateSelectAllButtonText(!allSelected);
    }
    
    @FXML
    private void handleEnrollment(ActionEvent event) {
        List<EnrollmentData> selectedSubjects = new ArrayList<>();
        
        // Collect all selected subjects and their schedules
        for (CheckBox checkbox : subjectCheckboxes) {
            if (checkbox.isSelected()) {
                ComboBox<String> scheduleCombo = checkboxScheduleMap.get(checkbox);
                String selectedSchedule = scheduleCombo.getValue();
                
                if (selectedSchedule == null || selectedSchedule.isEmpty()) {
                    showAlert("Missing Schedule", 
                             "Please select a schedule for all selected subjects.");
                    return;
                }
                
                SubjectData subject = checkboxSubjectMap.get(checkbox);
                selectedSubjects.add(new EnrollmentData(subject.code, selectedSchedule));
            }
        }
        
        if (selectedSubjects.isEmpty()) {
            showAlert("No Subjects Selected", 
                     "Please select at least one subject to enroll in.");
            return;
        }
        
        // Start background task to save enrollments
        enrollInSubjects(selectedSubjects);
    }

    private void enrollInSubjects(List<EnrollmentData> selectedSubjects) {
        // Display loading indicator
        showLoadingIndicator("Enrolling in subjects...");
        
        Task<Object> enrollmentTask = new Task<>() {
            @Override
            protected Object call() throws Exception {
                Connection connection = null;
                try {
                    // Get connection to database
                    connection = DBConnection.getConnection();
                    connection.setAutoCommit(false); // Start transaction
                    
                    // Get student ID from the email address
                    String[] credentials = RememberMeHandler.loadCredentials();
                    String studentEmail = credentials[0];
                    String studentId = null;
                    
                    // Query to get student id using case-insensitive email comparison
                    String studentQuery = "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)";
                    try (PreparedStatement stmt = connection.prepareStatement(studentQuery)) {
                        stmt.setString(1, studentEmail);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            studentId = rs.getString("student_id");
                        } else {
                            throw new Exception("Student not found with email: " + studentEmail);
                        }
                    }
                    
                    // Get current semester
                    String currentSemester = getCurrentSemester(connection);
                    
                    // Step 1: Get or create an enrollment header for this student and semester
                    Integer enrollmentId = getOrCreateEnrollmentHeader(connection, studentId, currentSemester);
                    
                    // Step 2: Insert each subject enrollment into student_load table
                    String insertDetailQuery = 
                        "INSERT INTO student_load (enrollment_id, subject_code, schedule, student_id) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT (enrollment_id, subject_code) DO NOTHING";
                        
                    try (PreparedStatement stmt = connection.prepareStatement(insertDetailQuery)) {
                        for (EnrollmentData enrollment : selectedSubjects) {
                            stmt.setInt(1, enrollmentId);
                            stmt.setString(2, enrollment.subjectCode);
                            stmt.setString(3, enrollment.schedule);
                            stmt.setString(4, studentId); // Add student_id to satisfy the constraint
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                    
                    // Commit transaction
                    connection.commit();
                    
                    // Return success message
                    return "Enrolled in " + selectedSubjects.size() + " subjects successfully!";
                    
                } catch (Exception e) {
                    // If there's an error, rollback the transaction
                    if (connection != null) {
                        try {
                            connection.rollback();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                } finally {
                    // Close the connection
                    if (connection != null) {
                        try {
                            connection.setAutoCommit(true);
                            connection.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        
        // Handle task completion
        enrollmentTask.setOnSucceeded(event -> {
            hideLoadingIndicator();
            Object result = enrollmentTask.getValue();
            if (result instanceof String) {
                String message = (String) result;
                if (message.startsWith("Error:")) {
                    showAlert("Enrollment Failed", message);
                } else {
                    showAlert("Enrollment Successful", message);
                }
            }
            // Refresh the subject list to reflect changes
            loadSubjects();
        });
        
        enrollmentTask.setOnFailed(event -> {
            hideLoadingIndicator();
            Throwable exception = enrollmentTask.getException();
            exception.printStackTrace();
            showAlert("Enrollment Failed", 
                    "Could not complete the enrollment process. Please try again later or contact support.\n\nError: " + 
                    exception.getMessage());
        });
        
        // Start the background task
        Thread thread = new Thread(enrollmentTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    private String getCurrentSemester(Connection connection) throws SQLException {
        // Default semester if not found
        String defaultSemester = "1st Semester 2025-2026";
        
        // Try to get the latest semester from the payment table
        String query = "SELECT semester FROM payment ORDER BY semester DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                String semester = rs.getString("semester");
                if (semester != null && !semester.isEmpty()) {
                    return semester;
                }
            }
        } catch (SQLException e) {
            // If table doesn't exist or query fails, just use default
            System.err.println("Could not determine current semester: " + e.getMessage());
        }
        
        return defaultSemester;
    }
    
    private void showLoadingIndicator(String message) {
        Platform.runLater(() -> {
            subjectListContainer.getChildren().clear();
            Label loadingLabel = new Label(message);
            loadingLabel.getStyleClass().add("loading-text");
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setMaxSize(30, 30);
            VBox loadingBox = new VBox(10, progressIndicator, loadingLabel);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            subjectListContainer.getChildren().add(loadingBox);
        });
    }
    
    private void hideLoadingIndicator() {
        Platform.runLater(() -> {
            subjectListContainer.getChildren().clear();
        });
    }

    private void updateSelectAllButtonState() {
        boolean allSelected = true;

        for (CheckBox checkbox : subjectCheckboxes) {
            if (!checkbox.isSelected()) {
                allSelected = false;
                break;
            }
        }

        updateSelectAllButtonText(allSelected);
    }

    private void updateSelectAllButtonText(boolean allSelected) {
        if (allSelected) {
            selectAllButton.setText("Deselect All");
        } else {
            selectAllButton.setText("Select All");
        }
    }

    private void loadSubjects() {
        // Clear existing contents
        subjectListContainer.getChildren().clear();
        subjectCheckboxes.clear();
        checkboxSubjectMap.clear();
        checkboxScheduleMap.clear();
        
        // Create a loading indicator
        Label loadingLabel = new Label("Loading subjects...");
        loadingLabel.getStyleClass().add("loading-text");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(30, 30);
        VBox loadingBox = new VBox(10, progressIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(20));
        subjectListContainer.getChildren().add(loadingBox);
        
        // Create a task for loading subjects in the background
        Task<List<SubjectData>> task = new Task<>() {
            @Override
            protected List<SubjectData> call() throws Exception {
                List<SubjectData> subjectDataList = new ArrayList<>();
                
                try (Connection connection = DBConnection.getConnection()) {
                    // Get the student's ID from the email
                    String[] credentials = RememberMeHandler.loadCredentials();
                    String studentEmail = credentials[0];
                    String studentId = null;

                    String getStudentIdQuery = "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)";
                    try (PreparedStatement stmt = connection.prepareStatement(getStudentIdQuery)) {
                        stmt.setString(1, studentEmail);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                studentId = rs.getString("student_id");
                            } else {
                                // Log error and return empty list
                                System.err.println("Student not found with email: " + studentEmail);
                                return subjectDataList;
                            }
                        }
                    }
                    
                    // Get current semester
                    String currentSemester = getCurrentSemester(connection);
                    
                    // Get subjects already enrolled in for this semester
                    Set<String> enrolledSubjects = new HashSet<>();
                    
                    // New query to get enrolled subjects using the enrollment_headers table
                    String enrolledQuery = 
                        "SELECT s.subject_code FROM student_load s " +
                        "JOIN enrollment_headers e ON s.enrollment_id = e.enrollment_id " +
                        "WHERE e.student_id = ? AND e.semester = ?";
                    
                    try (PreparedStatement stmt = connection.prepareStatement(enrolledQuery)) {
                        stmt.setString(1, studentId);
                        stmt.setString(2, currentSemester);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                enrolledSubjects.add(rs.getString("subject_code"));
                            }
                        }
                    }
                    
                    // Query for subjects, joining with faculty to get professor names
                    String query = "SELECT s.subject_code, s.description, " +
                            "f.firstname || ' ' || f.lastname AS professor_name " +
                            "FROM subjects s " +
                            "LEFT JOIN faculty f ON s.professor = f.faculty_id";
                    
                    try (Statement stmt = connection.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {
                        
                        while (rs.next()) {
                            String code = rs.getString("subject_code");
                            String description = rs.getString("description");
                            String professorName = rs.getString("professor_name");
                            
                            // Skip subjects that are already enrolled
                            if (enrolledSubjects.contains(code)) {
                                continue;
                            }
                            
                            // Add subject to the list
                            SubjectData subject = new SubjectData();
                            subject.code = code;
                            subject.description = description;
                            subject.professor = professorName;
                            
                            subjectDataList.add(subject);
                        }
                    }
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                return subjectDataList;
            }
        };
        
        // Handle task success
        task.setOnSucceeded(event -> {
            subjectListContainer.getChildren().clear(); // Remove the loading indicator
            List<SubjectData> subjects = task.getValue();
            
            if (subjects.isEmpty()) {
                showAlert("No Available Subjects", 
                        "There are no subjects available for enrollment at this time.");
                return;
            }
            
            // Create subject cards for each subject
            for (SubjectData subject : subjects) {
                addSubjectRow(subject);
            }
        });
        
        // Handle task failure
        task.setOnFailed(event -> {
            subjectListContainer.getChildren().clear(); // Remove the loading indicator
            Throwable exception = task.getException();
            exception.printStackTrace();
            showAlert("Database Error", 
                    "Could not retrieve subjects from database. Please try again later or contact support.\n\nError: " + 
                    exception.getMessage());
        });
        
        // Start the background task
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Set as daemon so it doesn't prevent application from exiting
        thread.start();
    }

    private void addSubjectRow(SubjectData subject) {
        // Create a row for the subject
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);
        row.setPadding(new Insets(5, 10, 5, 10));
        row.getStyleClass().add("subject-row");
        
        // Checkbox
        CheckBox checkbox = new CheckBox();
        checkbox.setPrefWidth(40);
        checkbox.getStyleClass().add("custom-checkbox");
        subjectCheckboxes.add(checkbox);
        
        // Store subject data with checkbox
        checkboxSubjectMap.put(checkbox, subject);
        
        // Subject code
        Label codeLabel = new Label(subject.code);
        codeLabel.setPrefWidth(200);
        codeLabel.getStyleClass().add("subject-code");
        
        // Description with professor
        String fullDescription = subject.description;
        if (subject.professor != null && !subject.professor.isEmpty()) {
            fullDescription += " - " + subject.professor;
        }
        Label descLabel = new Label(fullDescription);
        descLabel.setPrefWidth(300);
        descLabel.getStyleClass().add("subject-description");
        descLabel.setWrapText(true);
        
        // Schedule options
        ComboBox<String> scheduleCombo = new ComboBox<>();
        scheduleCombo.getItems().addAll("8:00 AM - 10:00 AM", "10:00 AM - 12:00 PM", "1:00 PM - 3:00 PM", "3:00 PM - 5:00 PM");
        scheduleCombo.setPrefWidth(180);
        scheduleCombo.setPrefHeight(30);
        scheduleCombo.setPromptText("Select Schedule");
        scheduleCombo.getStyleClass().addAll("modern-combo", "no-gray-disabled");
        scheduleCombo.setDisable(true);
        
        // Store schedule combobox with checkbox
        checkboxScheduleMap.put(checkbox, scheduleCombo);
        
        // Enable/disable schedule combo box based on checkbox state
        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            scheduleCombo.setDisable(!newValue);
            updateSelectAllButtonState();
        });
        
        // Add the components to the row
        row.getChildren().addAll(checkbox, codeLabel, descLabel, scheduleCombo);
        
        // Add the row to the container
        subjectListContainer.getChildren().add(row);
        
        // Add separator
        Separator separator = new Separator();
        subjectListContainer.getChildren().add(separator);
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Gets an existing enrollment header ID or creates a new one if it doesn't exist
     * @param connection The database connection
     * @param studentId The student ID
     * @param semester The current semester
     * @return The enrollment header ID
     */
    private Integer getOrCreateEnrollmentHeader(Connection connection, String studentId, String semester) 
            throws SQLException {
        // First try to get existing header
        String getHeaderSQL = 
            "SELECT enrollment_id FROM enrollment_headers " +
            "WHERE student_id = ? AND semester = ?";
            
        try (PreparedStatement stmt = connection.prepareStatement(getHeaderSQL)) {
            stmt.setString(1, studentId);
            stmt.setString(2, semester);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("enrollment_id");
            }
        }
        
        // If no header exists, create one
        String createHeaderSQL = 
            "INSERT INTO enrollment_headers (student_id, semester) " +
            "VALUES (?, ?) RETURNING enrollment_id";
            
        try (PreparedStatement stmt = connection.prepareStatement(createHeaderSQL)) {
            stmt.setString(1, studentId);
            stmt.setString(2, semester);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("enrollment_id");
            } else {
                throw new SQLException("Failed to create enrollment header");
            }
        }
    }

    // Helper class to store subject data
    private static class SubjectData {
        String code;
        String description;
        String professor;
        
        public SubjectData() {}
        
        public SubjectData(String code, String description, String professor) {
            this.code = code;
            this.description = description;
            this.professor = professor;
        }
    }
    
    // Helper class to store enrollment data
    private static class EnrollmentData {
        String subjectCode;
        String schedule;
        
        public EnrollmentData(String subjectCode, String schedule) {
            this.subjectCode = subjectCode;
            this.schedule = schedule;
        }
    }
}
