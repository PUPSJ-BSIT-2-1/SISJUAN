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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StudentEnrollmentController implements Initializable {

    @FXML private Button selectAllButton;
    @FXML private Button enrollButton;
    @FXML private VBox subjectListContainer;

    private List<CheckBox> subjectCheckboxes = new ArrayList<>();
    private Map<CheckBox, SubjectData> checkboxSubjectMap = new HashMap<>();
    private Map<CheckBox, ComboBox<String>> checkboxScheduleMap = new HashMap<>();

    // Common time slots for scheduling
    private static final String[] TIME_SLOTS = {
            "8:00 AM - 10:00 AM", "10:00 AM - 12:00 PM",
            "1:00 PM - 3:00 PM", "3:00 PM - 5:00 PM"
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSubjects();
        enrollButton.setOnAction(this::handleEnrollment);
    }

    @FXML
    private void handleSelectAll(ActionEvent event) {
        boolean allSelected = subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
        subjectCheckboxes.forEach(checkbox -> checkbox.setSelected(!allSelected));
        updateSelectAllButtonText(!allSelected);
    }

    @FXML
    private void handleEnrollment(ActionEvent event) {
        List<EnrollmentData> selectedSubjects = subjectCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> {
                    String subjectCode = checkboxSubjectMap.get(cb).code;
                    ComboBox<String> scheduleComboBox = checkboxScheduleMap.get(cb);
                    String schedule = scheduleComboBox.getValue() != null ? scheduleComboBox.getValue() : "Not Set";
                    return new EnrollmentData(subjectCode, schedule);
                })
                .collect(Collectors.toList());

        if (selectedSubjects.isEmpty()) {
            showAlert("No Subjects Selected", "Please select at least one subject to enroll.");
            return;
        }

        runBackgroundTask(
                "Enrolling in subjects...",
                () -> {
                    try (Connection connection = DBConnection.getConnection()) {
                        connection.setAutoCommit(false); // Start transaction

                        String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail(); // This might be an email or a student number
                        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
                            throw new Exception("No user is currently logged in. Please log in again.");
                        }

                        Integer studentIdInt = null;
                        String studentYearSection = null;
                        String query_StudentDetails;
                        boolean isEmailIdentifier = currentUserIdentifier.contains("@");

                        if (isEmailIdentifier) {
                            query_StudentDetails = "SELECT student_id, year_section FROM students WHERE LOWER(email) = LOWER(?)";
                        } else {
                            // Assuming currentUserIdentifier is the student_number
                            // USER: Confirm 'student_number' column and if 'year_section' should be fetched here.
                            query_StudentDetails = "SELECT student_id, year_section FROM students WHERE student_number = ?";
                        }

                        try (PreparedStatement ps = connection.prepareStatement(query_StudentDetails)) {
                            ps.setString(1, isEmailIdentifier ? currentUserIdentifier.toLowerCase() : currentUserIdentifier);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    studentIdInt = rs.getInt("student_id");
                                    studentYearSection = rs.getString("year_section"); // Assuming year_section is always present if student found
                                } else {
                                    throw new Exception("Student not found with identifier: " + currentUserIdentifier);
                                }
                            }
                        }

                        if (studentIdInt == null) { 
                            throw new Exception("Student ID could not be retrieved for identifier: " + currentUserIdentifier);
                        }

                        // Fetch student's integer ID and year_section
                        // Integer studentIdInt = null;
                        // String studentYearSection = null;

                        // try (PreparedStatement ps = connection.prepareStatement("SELECT student_id, year_section FROM students WHERE LOWER(email) = LOWER(?)")) {
                        //     ps.setString(1, studentEmail);
                        //     try (ResultSet rs = ps.executeQuery()) {
                        //         if (rs.next()) {
                        //             studentIdInt = rs.getInt("student_id");
                        //             studentYearSection = rs.getString("year_section");
                        //         } else {
                        //             throw new Exception("Student not found with email: " + studentEmail);
                        //         }
                        //     }
                        // }

                        // if (studentIdInt == null) { // Should be caught by rs.next() check, but as a safeguard
                        //     throw new Exception("Student ID could not be retrieved.");
                        // }

                        String currentSemester = getCurrentSemester(connection);
                        String academicYear = null;
                        if (currentSemester != null) {
                            String[] parts = currentSemester.split(" ");
                            if (parts.length > 0) {
                                academicYear = parts[parts.length - 1]; // Assumes year is last part e.g. "1st Semester 2023-2024"
                            }
                        }
                        if (academicYear == null) academicYear = "UNKNOWN"; // Fallback

                        // Get the next available load_id for student_load
                        int nextLoadId;
                        try (PreparedStatement ps = connection.prepareStatement("SELECT COALESCE(MAX(load_id), 0) + 1 AS next_id FROM student_load");
                             ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                nextLoadId = rs.getInt("next_id");
                            } else {
                                throw new SQLException("Could not determine next load_id for student_load.");
                            }
                        }

                        // Insert each subject enrollment into student_load table
                        String insertSql = "INSERT INTO student_load (student_id, subject_id, semester, load_id, academic_year, year_section, schedule) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (student_id, subject_id, semester) DO NOTHING";

                        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                            for (EnrollmentData enrollment : selectedSubjects) {
                                // Get subject_id (integer) from subject_code
                                Integer subjectIdInt = null;
                                try (PreparedStatement psSub = connection.prepareStatement("SELECT subject_id FROM subjects WHERE subject_code = ?")) {
                                    psSub.setString(1, enrollment.subjectCode);
                                    try (ResultSet rsSub = psSub.executeQuery()) {
                                        if (rsSub.next()) {
                                            subjectIdInt = rsSub.getInt("subject_id");
                                        } else {
                                            // Optionally, log or handle cases where subject_code is not found
                                            System.err.println("Subject code not found: " + enrollment.subjectCode + ". Skipping this enrollment.");
                                            continue; // Skip this subject
                                        }
                                    }
                                }

                                stmt.setInt(1, studentIdInt);
                                stmt.setInt(2, subjectIdInt);
                                stmt.setString(3, currentSemester);
                                stmt.setInt(4, nextLoadId++); // Use current nextLoadId and then increment
                                stmt.setString(5, academicYear);
                                stmt.setString(6, studentYearSection);
                                stmt.setString(7, enrollment.schedule); // Assuming 'schedule' column exists
                                stmt.addBatch();
                            }
                            stmt.executeBatch();
                        }

                        connection.commit();
                        return "Enrolled in " + selectedSubjects.size() + " subjects successfully!";

                    } catch (Exception e) {
                        // Rollback transaction in case of error
                        // Connection will be closed by try-with-resources, which might implicitly rollback if not committed
                        // Explicit rollback can be added if DBConnection.getConnection() doesn't handle auto-rollback on close for non-committed transactions.
                        System.err.println("Enrollment failed: " + e.getMessage());
                        e.printStackTrace();
                        throw e; // Re-throw to be caught by runBackgroundTask
                    }
                },
                result -> showAlert("Enrollment Status", (String) result)
        );
    }

    private void updateSelectAllButtonText(boolean allSelected) {
        selectAllButton.setText(allSelected ? "Deselect All" : "Select All");
    }

    private void loadSubjects() {
        runBackgroundTask(
                "Loading subjects...",
                this::fetchAvailableSubjects,
                subjects -> {
                    if (subjects == null || ((List<?>)subjects).isEmpty()) {
                        showAlert("No Available Subjects", "There are no subjects available for enrollment at this time.");
                        return;
                    }

                    // Create subject cards for each subject
                    for (SubjectData subject : (List<SubjectData>)subjects) {
                        addSubjectRow(subject);
                    }
                }
        );
    }

    private List<SubjectData> fetchAvailableSubjects() throws Exception {
        List<SubjectData> subjectDataList = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection()) {
            // Get the student's ID from the email
            String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail();
            
            // Check if we have a logged in user
            if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
                throw new Exception("No user is currently logged in. Please log in again.");
            }
            
            boolean isEmail = currentUserIdentifier.contains("@");
            String studentIdQuery;
            if (isEmail) {
                studentIdQuery = "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)";
            } else {
                // USER: Confirm 'student_number' is the correct column name.
                studentIdQuery = "SELECT student_id FROM students WHERE student_number = ?";
            }

            Integer studentIdInt = executeQuery(connection, 
                studentIdQuery,
                stmt -> stmt.setString(1, isEmail ? currentUserIdentifier.toLowerCase() : currentUserIdentifier),
                rs -> rs.next() ? rs.getInt("student_id") : null);

            if (studentIdInt == null) {
                System.err.println("Student not found with identifier: " + currentUserIdentifier);
                return subjectDataList; // Return empty list or throw error
            }

            // Get current semester
            String currentSemester = getCurrentSemester(connection);

            // Get subjects already enrolled in for this semester
            Set<String> enrolledSubjects = new HashSet<>();
            executeQuery(connection,
                    "SELECT sub.subject_code " +
                    "FROM student_load sl " +
                    "JOIN subjects sub ON sl.subject_id = sub.subject_id " +
                    "WHERE sl.student_id = ? AND sl.semester = ?",
                    stmt -> {
                        stmt.setInt(1, studentIdInt);
                        stmt.setString(2, currentSemester);
                    },
                    rs -> {
                        while (rs.next()) {
                            enrolledSubjects.add(rs.getString("subject_code"));
                        }
                        return null;
                    }
            );

            // Query for subjects, joining with faculty to get professor names
            return executeQuery(connection,
                    "SELECT s.subject_code, s.description, " +
                            "f.firstname || ' ' || f.lastname AS professor_name " +
                            "FROM subjects s " +
                            "LEFT JOIN faculty f ON s.faculty_id = f.faculty_id",
                    stmt -> {},
                    rs -> {
                        List<SubjectData> subjects = new ArrayList<>();
                        while (rs.next()) {
                            String code = rs.getString("subject_code");
                            // Skip subjects that are already enrolled
                            if (enrolledSubjects.contains(code)) continue;

                            SubjectData subject = new SubjectData(
                                    code,
                                    rs.getString("description"),
                                    rs.getString("professor_name")
                            );
                            subjects.add(subject);
                        }
                        return subjects;
                    }
            );
        }
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

        // Schedule options
        ComboBox<String> scheduleCombo = new ComboBox<>();
        scheduleCombo.getItems().addAll(TIME_SLOTS);
        scheduleCombo.setPrefWidth(180);
        scheduleCombo.setPrefHeight(30);
        scheduleCombo.setPromptText("Select Schedule");
        scheduleCombo.getStyleClass().addAll("modern-combo", "no-gray-disabled");
        scheduleCombo.setDisable(true);

        // Store data with checkbox
        checkboxSubjectMap.put(checkbox, subject);
        checkboxScheduleMap.put(checkbox, scheduleCombo);

        // Enable/disable schedule combo box based on checkbox state
        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            scheduleCombo.setDisable(!newValue);
            boolean allSelected = subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
            updateSelectAllButtonText(allSelected);
        });

        // Create subject info labels
        Label codeLabel = new Label(subject.code);
        codeLabel.setPrefWidth(200);
        codeLabel.getStyleClass().add("subject-code");

        String fullDescription = subject.description;
        if (subject.professor != null && !subject.professor.isEmpty()) {
            fullDescription += " - " + subject.professor;
        }
        Label descLabel = new Label(fullDescription);
        descLabel.setPrefWidth(300);
        descLabel.getStyleClass().add("subject-description");
        descLabel.setWrapText(true);

        // Add the components to the row and container
        row.getChildren().addAll(checkbox, codeLabel, descLabel, scheduleCombo);
        subjectListContainer.getChildren().addAll(row, new Separator());
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

    private String getCurrentSemester(Connection connection) throws SQLException {
        String defaultSemester = "1st Semester 2025-2026";

        try {
            return executeQuery(connection,
                    "SELECT semester FROM payment ORDER BY semester DESC LIMIT 1",
                    stmt -> {},
                    rs -> rs.next() && rs.getString("semester") != null ? rs.getString("semester") : defaultSemester
            );
        } catch (SQLException e) {
            System.err.println("Could not determine current semester: " + e.getMessage());
            return defaultSemester;
        }
    }

    // Generic method to display a loading indicator and run a task in the background
    private <T> void runBackgroundTask(String loadingMessage, TaskSupplier<T> taskSupplier, Consumer<T> onSuccess) {
        Platform.runLater(() -> {
            subjectListContainer.getChildren().clear();
            VBox loadingBox = new VBox(10,
                    createProgressIndicator(),
                    createStyledLabel(loadingMessage, "loading-text"));
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(20));
            subjectListContainer.getChildren().add(loadingBox);
        });

        Task<T> task = getTTask(taskSupplier, onSuccess);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private <T> Task<T> getTTask(TaskSupplier<T> taskSupplier, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return taskSupplier.get();
            }
        };

        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                subjectListContainer.getChildren().clear();
                if (onSuccess != null) onSuccess.accept(task.getValue());
            });
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                subjectListContainer.getChildren().clear();
                Throwable exception = task.getException();
                exception.printStackTrace();
                showAlert("Error", "An error occurred: " + exception.getMessage());
            });
        });
        return task;
    }

    // Helper method to create a progress indicator
    private ProgressIndicator createProgressIndicator() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(30, 30);
        return indicator;
    }

    // Helper method to create a styled label
    private Label createStyledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    // Generic method to execute database queries with proper resource management
    private <T> T executeQuery(Connection connection, String sql,
                               SQLConsumer<PreparedStatement> paramSetter,
                               SQLFunction<ResultSet, T> resultHandler) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            paramSetter.accept(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                return resultHandler.apply(rs);
            }
        }
    }

    // Functional interfaces for database operations
    @FunctionalInterface
    private interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }

    @FunctionalInterface
    private interface SQLFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    @FunctionalInterface
    private interface TaskSupplier<T> {
        T get() throws Exception;
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
