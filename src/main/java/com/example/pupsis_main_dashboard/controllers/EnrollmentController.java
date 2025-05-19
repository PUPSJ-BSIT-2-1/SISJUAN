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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class EnrollmentController implements Initializable {

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
        List<EnrollmentData> selectedSubjects = new ArrayList<>();

        // Collect all selected subjects and their schedules
        for (CheckBox checkbox : subjectCheckboxes) {
            if (checkbox.isSelected()) {
                ComboBox<String> scheduleCombo = checkboxScheduleMap.get(checkbox);
                String selectedSchedule = scheduleCombo.getValue();

                if (selectedSchedule == null || selectedSchedule.isEmpty()) {
                    showAlert("Missing Schedule", "Please select a schedule for all selected subjects.");
                    return;
                }

                SubjectData subject = checkboxSubjectMap.get(checkbox);
                selectedSubjects.add(new EnrollmentData(subject.code, selectedSchedule));
            }
        }

        if (selectedSubjects.isEmpty()) {
            showAlert("No Subjects Selected", "Please select at least one subject to enroll in.");
            return;
        }

        // Start background task to save enrollments
        runBackgroundTask(
                "Enrolling in subjects...",
                () -> enrollInSubjects(selectedSubjects),
                result -> {
                    if (result.toString().startsWith("Error:")) {
                        showAlert("Enrollment Failed", result.toString());
                    } else {
                        showAlert("Enrollment Successful", result.toString());
                    }
                    loadSubjects(); // Refresh the subject list
                }
        );
    }

    private Object enrollInSubjects(List<EnrollmentData> selectedSubjects) throws Exception {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); // Start transaction

            // Get student ID from the email address
            String studentEmail = RememberMeHandler.getCurrentUserEmail();

            // Query to get student id using case-insensitive email comparison
            String studentId = executeQuery(connection,
                    "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)",
                    stmt -> stmt.setString(1, studentEmail),
                    rs -> rs.next() ? rs.getString("student_id") : null
            );

            if (studentId == null) {
                throw new Exception("Student not found with email: " + studentEmail);
            }

            // Get current semester
            String currentSemester = getCurrentSemester(connection);

            // Get or create an enrollment header
            Integer enrollmentId = getOrCreateEnrollmentHeader(connection, studentId, currentSemester);

            // Insert each subject enrollment into student_load table
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO student_load (enrollment_id, subject_code, schedule, student_id) " +
                            "VALUES (?, ?, ?, ?) ON CONFLICT (enrollment_id, subject_code) DO NOTHING")) {

                for (EnrollmentData enrollment : selectedSubjects) {
                    stmt.setInt(1, enrollmentId);
                    stmt.setString(2, enrollment.subjectCode);
                    stmt.setString(3, enrollment.schedule);
                    stmt.setString(4, studentId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            connection.commit();
            return "Enrolled in " + selectedSubjects.size() + " subjects successfully!";

        } catch (Exception e) {
            throw e;
        }
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
            String studentEmail = RememberMeHandler.getCurrentUserEmail();
            
            // Check if we have a logged in user
            if (studentEmail == null || studentEmail.isEmpty()) {
                throw new Exception("No user is currently logged in. Please log in again.");
            }
            
            String studentId = executeQuery(connection, 
                "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)",
                stmt -> stmt.setString(1, studentEmail),
                rs -> rs.next() ? rs.getString("student_id") : null);


            if (studentId == null) {
                System.err.println("Student not found with email: " + studentEmail);
                return subjectDataList;
            }

            // Get current semester
            String currentSemester = getCurrentSemester(connection);

            // Get subjects already enrolled in for this semester
            Set<String> enrolledSubjects = new HashSet<>();
            executeQuery(connection,
                    "SELECT s.subject_code FROM student_load s " +
                            "JOIN enrollment_headers e ON s.enrollment_id = e.enrollment_id " +
                            "WHERE e.student_id = ? AND e.semester = ?",
                    stmt -> {
                        stmt.setString(1, studentId);
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
                            "LEFT JOIN faculty f ON s.professor = f.faculty_id",
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

    private Integer getOrCreateEnrollmentHeader(Connection connection, String studentId, String semester)
            throws SQLException {
        // First try to get existing header
        Integer headerID = executeQuery(connection,
                "SELECT enrollment_id FROM enrollment_headers WHERE student_id = ? AND semester = ?",
                stmt -> {
                    stmt.setString(1, studentId);
                    stmt.setString(2, semester);
                },
                rs -> rs.next() ? rs.getInt("enrollment_id") : null
        );

        if (headerID != null) return headerID;

        // If no header exists, create one
        return executeQuery(connection,
                "INSERT INTO enrollment_headers (student_id, semester) VALUES (?, ?) RETURNING enrollment_id",
                stmt -> {
                    stmt.setString(1, studentId);
                    stmt.setString(2, semester);
                },
                rs -> {
                    if (rs.next()) return rs.getInt("enrollment_id");
                    throw new SQLException("Failed to create enrollment header");
                }
        );
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
