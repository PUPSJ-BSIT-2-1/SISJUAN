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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StudentEnrollmentController implements Initializable {

    @FXML private VBox subjectListContainer;
    @FXML private Button selectAllButton;
    @FXML private Button enrollButton;
    @FXML private Label currentYearLevelDisplayLabel;
    @FXML private Label currentSemesterDisplayLabel;
    @FXML private ProgressIndicator loadingIndicator; // Make sure fx:id="loadingIndicator" is in FXML

    // Instance variables
    private StudentEnrollmentContext studentEnrollmentContext; // Ensure this declaration exists
    private List<SubjectData> availableSubjects;
    private List<CheckBox> subjectCheckboxes = new ArrayList<>();
    private Map<CheckBox, SubjectData> checkboxSubjectMap = new HashMap<>();
    private Map<CheckBox, ComboBox<String>> checkboxScheduleMap = new HashMap<>();

    // Define TIME_SLOTS - this should be populated appropriately, e.g., from a utility or constants class
    private static final List<String> TIME_SLOTS = Arrays.asList(
            "Mon/Wed 9:00-10:30 AM",
            "Tue/Thu 1:00-2:30 PM",
            "Fri 9:00-12:00 PM"
            // Add more actual time slots
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        enrollButton.setDisable(true);
        selectAllButton.setDisable(true);
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        Task<StudentEnrollmentContext> fetchContextTask = new Task<>() {
            @Override
            protected StudentEnrollmentContext call() throws Exception {
                return fetchStudentEnrollmentContextLogic();
            }
        };

        fetchContextTask.setOnSucceeded(event -> {
            StudentEnrollmentContext context = fetchContextTask.getValue();
            if (context != null) {
                updateContextUIDisplay(context);
                loadSubjectsInBackground(context);
            } else {
                handleContextLoadingErrorUI();
            }
        });

        fetchContextTask.setOnFailed(event -> {
            handleContextLoadingErrorUI();
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            Throwable ex = fetchContextTask.getException();
            if (ex != null) {
                ex.printStackTrace();
                // Optionally show an alert with ex.getMessage()
            }
        });

        new Thread(fetchContextTask).start();

        if (enrollButton != null) {
             enrollButton.setOnAction(this::handleEnrollment); // Or handleEnrollAction if that's the correct method
        }
        updateSelectAllButtonState(); // Initial state
    }

    private void updateContextUIDisplay(StudentEnrollmentContext context) {
        if (currentYearLevelDisplayLabel != null) {
            currentYearLevelDisplayLabel.setText(context.yearLevelString);
        }
        if (currentSemesterDisplayLabel != null) {
            currentSemesterDisplayLabel.setText(context.semesterString);
        }
    }

    private void handleContextLoadingErrorUI() {
        // This method is already on FX thread if called from setOnSucceeded/setOnFailed
        if (currentYearLevelDisplayLabel != null) currentYearLevelDisplayLabel.setText("N/A");
        if (currentSemesterDisplayLabel != null) currentSemesterDisplayLabel.setText("N/A");
        showAlert("Error", "Could not load student enrollment details. Please check your profile or contact admin.");
        subjectListContainer.getChildren().add(new Label("Could not load subjects. Please ensure your section and semester are correctly set."));
        if (enrollButton != null) enrollButton.setDisable(true);
        if (selectAllButton != null) selectAllButton.setDisable(true);
    }

    private void loadSubjectsInBackground(StudentEnrollmentContext context) {
        if (context == null) {
            showAlert("Error", "Cannot load subjects without student context.");
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            return;
        }

        if (loadingIndicator != null) loadingIndicator.setVisible(true);
        enrollButton.setDisable(true);
        selectAllButton.setDisable(true);

        Task<List<SubjectData>> loadSubjectsTask = new Task<>() {
            @Override
            protected List<SubjectData> call() throws Exception {
                return loadSubjectsLogic(context);
            }
        };

        loadSubjectsTask.setOnSucceeded(event -> {
            List<SubjectData> loadedSubjects = loadSubjectsTask.getValue();
            populateSubjectListUI(loadedSubjects, context);
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            enrollButton.setDisable(false);
            selectAllButton.setDisable(false);
        });

        loadSubjectsTask.setOnFailed(event -> {
            showAlert("Error", "Failed to load subjects.");
            subjectListContainer.getChildren().add(new Label("Failed to load subjects."));
            if (enrollButton != null) enrollButton.setDisable(true);
            if (selectAllButton != null) selectAllButton.setDisable(true);
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            Throwable ex = loadSubjectsTask.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        new Thread(loadSubjectsTask).start();
    }

    private StudentEnrollmentContext fetchStudentEnrollmentContextLogic() throws SQLException {
        String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail();
        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            // This case should ideally be handled before even starting the task,
            // or throw a specific exception to be caught by setOnFailed.
            System.err.println("No user logged in. Cannot load enrollment details.");
            return null; // Or throw new IllegalStateException("No user logged in.");
        }

        String sql;
        boolean isEmail = currentUserIdentifier.contains("@");
        if (isEmail) {
            sql = "SELECT s.year_section AS student_year_section, ys.semester AS section_semester " +
                  "FROM students s " +
                  "LEFT JOIN year_section ys ON s.year_section = ys.year_section " +
                  "WHERE LOWER(s.email) = LOWER(?)";
        } else {
            sql = "SELECT s.year_section AS student_year_section, ys.semester AS section_semester " +
                  "FROM students s " +
                  "LEFT JOIN year_section ys ON s.year_section = ys.year_section " +
                  "WHERE s.student_number = ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUserIdentifier);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String studentYearSection = rs.getString("student_year_section");
                    String sectionSemester = rs.getString("section_semester");

                    if (studentYearSection == null || studentYearSection.trim().isEmpty()) {
                        // Throw exception or return null to indicate failure
                        throw new SQLException("Student section not set.");
                    }
                    if (sectionSemester == null || sectionSemester.trim().isEmpty()) {
                        throw new SQLException("Semester for section '" + studentYearSection + "' not defined.");
                    }

                    String[] parts = studentYearSection.split("-");
                    if (parts.length > 0 && parts[0].matches("\\d+")) {
                        int numericYearLevel = Integer.parseInt(parts[0]);
                        String yearLevelString = convertNumericYearToString(numericYearLevel);
                        String standardizedSemester = standardizeSemesterFormat(sectionSemester);

                        if (yearLevelString == null) {
                            throw new SQLException("Could not determine year level string from section: " + studentYearSection);
                        }
                        if (standardizedSemester == null) {
                            throw new SQLException("Could not standardize semester from section's semester: " + sectionSemester);
                        }
                        return new StudentEnrollmentContext(yearLevelString, standardizedSemester);
                    } else {
                        throw new SQLException("Invalid section format: " + studentYearSection);
                    }
                }
            }
        }
        return null; // Student not found or other issue
    }

    private List<SubjectData> loadSubjectsLogic(StudentEnrollmentContext context) throws SQLException {
        List<SubjectData> subjectDataList = new ArrayList<>();
        String sql = "SELECT subject_id, subject_code, description, units FROM subjects WHERE LOWER(year_level) = LOWER(?) AND LOWER(semester) = LOWER(?) ORDER BY description";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, context.yearLevelString); // Already lowercased by SQL LOWER()
            pstmt.setString(2, context.semesterString);    // Already lowercased by SQL LOWER()

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subjectDataList.add(new SubjectData(
                            rs.getString("subject_code"),
                            rs.getString("description"),
                            rs.getInt("units"),
                            rs.getInt("subject_id")
                    ));
                }
            }
        }
        return subjectDataList;
    }

    private void populateSubjectListUI(List<SubjectData> loadedSubjects, StudentEnrollmentContext context) {
        subjectListContainer.getChildren().clear();
        subjectCheckboxes.clear();
        checkboxSubjectMap.clear();
        checkboxScheduleMap.clear();

        if (loadedSubjects.isEmpty()) {
            Label infoLabel = new Label("No subjects available for your year level ('" + context.yearLevelString +
                                        "') and semester ('" + context.semesterString + "').");
            infoLabel.setPadding(new Insets(10));
            subjectListContainer.getChildren().add(infoLabel);
        } else {
            for (SubjectData subject : loadedSubjects) {
                CheckBox checkBox = new CheckBox(); // Text will be handled by separate labels
                checkBox.getStyleClass().add("custom-checkbox");
                HBox.setMargin(checkBox, new Insets(0, 0, 0, 5));
                checkBox.setOnAction(event -> updateSelectAllButtonState());

                Label subjectCodeLabel = new Label(subject.code);
                subjectCodeLabel.setPrefWidth(200.0);
                subjectCodeLabel.getStyleClass().add("subject-code");

                Label descriptionLabel = new Label(subject.name); // subject.name maps to DB's description column
                descriptionLabel.setPrefWidth(300.0);
                descriptionLabel.getStyleClass().add("subject-description");
                descriptionLabel.setWrapText(true);

                ComboBox<String> scheduleComboBox = new ComboBox<>();
                scheduleComboBox.getItems().addAll(TIME_SLOTS); 
                scheduleComboBox.setPromptText("Select Schedule");
                scheduleComboBox.setPrefHeight(30.0);
                scheduleComboBox.setPrefWidth(180.0);
                scheduleComboBox.getStyleClass().add("modern-combo");

                HBox subjectRow = new HBox(10); // Spacing is 10
                subjectRow.setAlignment(Pos.CENTER_LEFT);
                subjectRow.getStyleClass().add("subject-row");
                subjectRow.setPadding(new Insets(5, 0, 5, 0)); // top, right, bottom, left
                
                subjectRow.getChildren().addAll(checkBox, subjectCodeLabel, descriptionLabel, scheduleComboBox);

                subjectListContainer.getChildren().add(subjectRow);
                subjectCheckboxes.add(checkBox);
                checkboxSubjectMap.put(checkBox, subject);
                checkboxScheduleMap.put(checkBox, scheduleComboBox);
            }
            selectAllButton.setDisable(false);
        }
        updateSelectAllButtonState(); // Initial state for the button text
    }

    @FXML
    private void handleSelectAll() {
        boolean allSelected = subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
        boolean newSelectedState = !allSelected;
        for (CheckBox cb : subjectCheckboxes) {
            cb.setSelected(newSelectedState);
        }
        updateSelectAllButtonState();
    }

    private void updateSelectAllButtonState() {
        if (subjectCheckboxes.isEmpty()) {
            selectAllButton.setText("Select All");
            selectAllButton.setDisable(true);
        } else {
            selectAllButton.setDisable(false);
            boolean allSelected = subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
            selectAllButton.setText(allSelected ? "Deselect All" : "Select All");
        }
    }

    @FXML
    private void handleEnrollment(ActionEvent event) { 
        List<EnrollmentData> selectedSubjects = subjectCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> {
                    SubjectData subjectData = checkboxSubjectMap.get(cb);
                    ComboBox<String> scheduleBox = checkboxScheduleMap.get(cb);
                    String selectedSchedule = scheduleBox.getValue() != null ? scheduleBox.getValue() : "Not Set";
                    if ("Select Schedule".equals(selectedSchedule) || selectedSchedule.trim().isEmpty()) { // Treat placeholder or empty as "Not Set"
                        selectedSchedule = "Not Set";
                    }
                    return new EnrollmentData(subjectData.code, subjectData.name, selectedSchedule, subjectData.id);
                })
                .collect(Collectors.toList());

        // Check for unselected schedules before proceeding
        boolean allSchedulesSet = selectedSubjects.stream().noneMatch(data -> "Not Set".equals(data.getSchedule()));
        if (!allSchedulesSet && !selectedSubjects.isEmpty()) { // Only warn if subjects are selected but schedules are missing
            showAlert("Schedule Selection Required", "Please select a schedule for all chosen subjects before enrolling.", Alert.AlertType.WARNING);
            return;
        }

        if (selectedSubjects.isEmpty()) {
            showAlert("No Subjects Selected", "Please select at least one subject to enroll.", Alert.AlertType.WARNING);
            return;
        }

        enrollButton.setDisable(true);
        selectAllButton.setDisable(true);
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        Task<List<EnrollmentData>> enrollmentTask = new Task<>() { // Return List<EnrollmentData>
            @Override
            protected List<EnrollmentData> call() throws Exception {
                // Fetch student ID (ensure this logic is robust)
                String currentUserIdentifier = RememberMeHandler.getCurrentUserEmail(); // Or student number
                Integer studentId = getStudentIdByIdentifier(currentUserIdentifier);
                if (studentId == null) {
                    throw new SQLException("Could not retrieve student ID for enrollment.");
                }

                String studentYearSection = fetchStudentYearSection(studentId);
                if (studentYearSection == null) {
                    throw new SQLException("Could not retrieve student year section for enrollment.");
                }

                String academicYear = getCurrentAcademicYear(); // Implement this method
                String currentSemesterForDB = standardizeSemesterFormat(currentSemesterDisplayLabel.getText());
                if (currentSemesterForDB == null || currentSemesterForDB.equals("N/A")) {
                    currentSemesterForDB = studentEnrollmentContext != null ? studentEnrollmentContext.getSemesterString() : "Unknown Semester";
                    currentSemesterForDB = standardizeSemesterFormat(currentSemesterForDB); // Standardize again
                }
                if (currentSemesterForDB == null) { // Final fallback
                     throw new SQLException("Could not determine current semester for enrollment.");
                }


                try (Connection connection = DBConnection.getConnection()) {
                    connection.setAutoCommit(false); // Start transaction

                    // Get the next load_id
                    int nextLoadId = 0;
                    String maxLoadIdSql = "SELECT MAX(load_id) FROM student_load";
                    try (PreparedStatement maxStmt = connection.prepareStatement(maxLoadIdSql);
                         ResultSet rs = maxStmt.executeQuery()) {
                        if (rs.next()) {
                            nextLoadId = rs.getInt(1) + 1;
                        }
                        if (nextLoadId == 0) nextLoadId = 1; // Start from 1 if table is empty
                    }

                    String insertSql = "INSERT INTO student_load (student_id, subject_id, semester, load_id, academic_year, year_section) " +
                            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (student_id, subject_id, semester) DO NOTHING";

                    try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                        for (EnrollmentData enrollment : selectedSubjects) {
                            stmt.setInt(1, studentId);
                            stmt.setInt(2, enrollment.getSubjectId());
                            stmt.setString(3, currentSemesterForDB);
                            stmt.setInt(4, nextLoadId++);
                            stmt.setString(5, academicYear);
                            stmt.setString(6, studentYearSection);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                    connection.commit(); // Commit transaction
                    return selectedSubjects; // Return the list of processed subjects
                } catch (SQLException e) {
                    // connection.rollback(); // Rollback on error if autoCommit was false
                    throw e; // Re-throw to be caught by onFailed
                }
            }
        };

        enrollmentTask.setOnSucceeded(workerStateEvent -> {
            List<EnrollmentData> enrolledItems = enrollmentTask.getValue();
            StringBuilder successMessage = new StringBuilder("Successfully enrolled in the following subjects:\n\n");
            for (EnrollmentData item : enrolledItems) {
                successMessage.append("- ").append(item.getSubjectCode()).append(" (").append(item.getSubjectName()).append("): ")
                              .append(item.getSchedule()).append("\n");
            }
            showAlert("Enrollment Successful", successMessage.toString(), Alert.AlertType.INFORMATION);
            clearEnrollmentUIState();
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            enrollButton.setDisable(false);
            selectAllButton.setDisable(false);
        });

        enrollmentTask.setOnFailed(workerStateEvent -> {
            Throwable exception = enrollmentTask.getException();
            System.err.println("Enrollment failed: " + exception.getMessage());
            exception.printStackTrace();
            showAlert("Enrollment Failed", "An error occurred during enrollment: " + exception.getMessage(), Alert.AlertType.ERROR);
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            enrollButton.setDisable(false);
            selectAllButton.setDisable(false);
        });

        new Thread(enrollmentTask).start();
    }

    private void clearEnrollmentUIState() {
        for (CheckBox cb : subjectCheckboxes) {
            cb.setSelected(false);
        }
        for (ComboBox<String> cbBox : checkboxScheduleMap.values()) {
            cbBox.getSelectionModel().clearSelection();
            cbBox.setPromptText("Select Schedule"); // Reset prompt text if needed
        }
        updateSelectAllButtonState(); // This will set text to "Select All" and enable button
    }

    // Helper method to get student_id (implement robustly)
    private Integer getStudentIdByIdentifier(String identifier) throws SQLException {
        String sql = identifier.contains("@") ? "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)" :
                                              "SELECT student_id FROM students WHERE student_number = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, identifier);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("student_id");
                }
            }
        }
        return null;
    }

    // Helper method to fetch student's year_section (implement robustly)
    private String fetchStudentYearSection(int studentId) throws SQLException {
        String sql = "SELECT year_section FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("year_section");
                }
            }
        }
        return null;
    }

    // Helper method to get current academic year (implement as needed)
    private String getCurrentAcademicYear() {
        // Example: "2025-2026". This might come from a global setting or date calculation.
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        // Assuming academic year starts around August/September (month 7 or 8)
        if (month >= Calendar.AUGUST) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(alertType);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }

    // Original showAlert now defaults to ERROR
    private void showAlert(String title, String message) {
        showAlert(title, message, Alert.AlertType.ERROR);
    }

    private static class EnrollmentData {
        String subjectCode;
        String subjectName;
        String schedule;
        int subjectId;

        public EnrollmentData(String subjectCode, String subjectName, String schedule, int subjectId) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.schedule = schedule;
            this.subjectId = subjectId;
        }

        public String getSubjectCode() { return subjectCode; }
        public String getSubjectName() { return subjectName; }
        public String getSchedule() { return schedule; }
        public int getSubjectId() { return subjectId; }
    }

    // Inner class to hold student context
    private static class StudentEnrollmentContext {
        final String yearLevelString;
        final String semesterString;

        StudentEnrollmentContext(String yearLevelString, String semesterString) {
            this.yearLevelString = yearLevelString;
            this.semesterString = semesterString;
        }
        
        public String getYearLevelString() {
            return yearLevelString;
        }
        
        public String getSemesterString() {
            return semesterString;
        }
    }

    private static class SubjectData {
        final String code;
        final String name;
        final int units;
        final int id; // subject_id

        SubjectData(String code, String name, int units, int id) {
            this.code = code;
            this.name = name;
            this.units = units;
            this.id = id;
        }
    }

    private String convertNumericYearToString(int numericYear) {
        switch (numericYear) {
            case 1: return "1st year";
            case 2: return "2nd year";
            case 3: return "3rd year";
            case 4: return "4th year";
            case 5: return "5th year";
            default: return null;
        }
    }

    private String standardizeSemesterFormat(String dbSemester) {
        if (dbSemester == null) return null;
        String lowerDbSemester = dbSemester.toLowerCase().trim();
        if (lowerDbSemester.contains("1st") || lowerDbSemester.contains("first") || lowerDbSemester.equals("1")) return "1st semester";
        if (lowerDbSemester.contains("2nd") || lowerDbSemester.contains("second") || lowerDbSemester.equals("2")) return "2nd semester";
        if (lowerDbSemester.contains("3rd") || lowerDbSemester.contains("third") || lowerDbSemester.equals("3")) return "3rd semester";
        return null;
    }
}
