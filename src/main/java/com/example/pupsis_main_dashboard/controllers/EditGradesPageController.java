package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Student;
import com.example.pupsis_main_dashboard.utilities.DBConnection; // Assuming DBConnection, adjust if needed
// import com.example.pupsis_main_dashboard.utilities.StudentCache; // Temporarily commented out
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.text.DecimalFormat;

public class EditGradesPageController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(EditGradesPageController.class);

    @FXML private TextField searchBar;
    @FXML private Label gradesHeaderlbl;
    @FXML private Label subDesclbl;
    @FXML private Label subjDescLbl;
    @FXML private MenuButton subjCodeCombBox;
    @FXML private MenuButton yrSecCombBox;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> noStudCol;
    @FXML private TableColumn<Student, String> studIDCol;
    @FXML private TableColumn<Student, String> studNameCol;
    @FXML private TableColumn<Student, String> subjCodeCol;
    @FXML private TableColumn<Student, String> finGradeCol;
    @FXML private TableColumn<Student, String> gradeStatCol;

    private final ObservableList<Student> studentsList = FXCollections.observableArrayList();
    // private final StudentCache studentCache = StudentCache.getInstance(); // Temporarily commented out
    private String selectedSubjectCode;
    private String selectedSubjectDesc;

    // Add DecimalFormat for consistent grade formatting
    private final DecimalFormat gradeFormat = new DecimalFormat("0.00");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing EditGradesPageController...");
        if (studentsTable == null) {
            System.err.println("Error: studentsTable is null. Check FXML file for proper fx:id.");
            logger.error("studentsTable is null. FXML fx:id might be missing or incorrect.");
            return;
        }

        setupRowHoverEffect();

        // Make table editable
        studentsTable.setEditable(true);
        logger.debug("Students table set to editable.");

        // Initialize the columns with the correct property names
        noStudCol.setCellValueFactory(new PropertyValueFactory<>("studentNo")); // Student's official number
        studIDCol.setCellValueFactory(new PropertyValueFactory<>("studentNo")); // Displaying studentNo as ID as well
        studNameCol.setCellValueFactory(new PropertyValueFactory<>("studentFullName"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCodeForGrade"));
        finGradeCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        gradeStatCol.setCellValueFactory(new PropertyValueFactory<>("gradeStatusName"));
        logger.debug("Table columns initialized with PropertyValueFactory.");

        // Make final grade column editable with custom cell factory
        finGradeCol.setCellFactory(tc -> new TableCell<Student, String>() {
            private TextField textField;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                // logger.trace("updateItem called for cell. Item: {}, Empty: {}", item, empty); // Can be too verbose
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    try {
                        float gradeValue = Float.parseFloat(item);
                        setText(gradeFormat.format(gradeValue));
                    } catch (NumberFormatException e) {
                        setText(item); 
                        logger.warn("NumberFormatException while formatting grade for display: {}", item, e);
                    }
                    setGraphic(null);
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();

                logger.debug("startEdit called for grade cell.");
                if (textField == null) {
                    createTextField();
                }
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                logger.debug("cancelEdit called for grade cell. Current item: {}", getItem());
                try {
                    float gradeValue = Float.parseFloat(getItem());
                    setText(gradeFormat.format(gradeValue));
                } catch (NumberFormatException e) {
                    setText(getItem());
                    logger.warn("NumberFormatException during cancelEdit formatting: {}", getItem(), e);
                }
                setGraphic(null);
            }

            private void createTextField() {
                logger.debug("Creating TextField for grade editing. Initial item: {}", getItem());
                String initialValue = getItem();
                try {
                    float gradeValue = Float.parseFloat(initialValue);
                    initialValue = gradeFormat.format(gradeValue);
                } catch (NumberFormatException e) {
                    logger.warn("NumberFormatException creating TextField, using original value: {}", initialValue, e);
                }

                textField = new TextField(initialValue);
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        logger.debug("Enter key pressed in grade TextField. Value: {}", textField.getText());
                        if (isValidGrade(textField.getText())) {
                            String formattedGrade = formatGradeInput(textField.getText());
                            logger.debug("Committing edit with formatted grade: {}", formattedGrade);
                            commitEdit(formattedGrade);
                        } else {
                            logger.warn("Invalid grade entered: {}. Cancelling edit.", textField.getText());
                            cancelEdit();
                            showError("Invalid Grade", "Please enter a valid grade between 1.00 and 5.00");
                        }
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        logger.debug("Escape key pressed in grade TextField. Cancelling edit.");
                        cancelEdit();
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    // logger.trace("TextField focus changed. WasFocused: {}, IsNowFocused: {}", wasFocused, isNowFocused); // Can be verbose
                    if (!isNowFocused && wasFocused) { // Commit only if it was focused and now lost focus
                        logger.debug("TextField lost focus. Value: {}", textField.getText());
                        if (isValidGrade(textField.getText())) {
                            String formattedGrade = formatGradeInput(textField.getText());
                            logger.debug("Committing edit due to focus loss with formatted grade: {}", formattedGrade);
                            commitEdit(formattedGrade);
                        } else {
                            logger.warn("Invalid grade on focus loss: {}. Cancelling edit.", textField.getText());
                            cancelEdit();
                            // Avoid showing error on every focus loss if invalid, could be annoying.
                            // Consider if error should only be shown on explicit commit (Enter key).
                        }
                    }
                });
            }
        });
        logger.debug("Final grade column CellFactory set up for editing.");

        // Handle the commit of edits
        finGradeCol.setOnEditCommit(event -> {
            Student student = event.getRowValue();
            String newGrade = event.getNewValue();
            logger.info("Final grade edit committed for student: {} (ID: {}), New Grade: {}", student.getStudentFullName(), student.getStudentNo(), newGrade);
            student.setFinalGrade(newGrade);
            updateGradeInDatabase(student);
        });

        // Make other columns non-editable
        noStudCol.setEditable(false);
        studIDCol.setEditable(false);
        studNameCol.setEditable(false);
        subjCodeCol.setEditable(false);
        gradeStatCol.setEditable(false);

        // Prevent column reordering
        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
        logger.debug("Column reordering disabled.");

        // Populate the subject codes dropdown
        populateSubjectCodes();

        // If a subject code was set before initialization, load it now
        if (selectedSubjectCode != null && selectedSubjectDesc != null) {
            logger.info("Pre-selected subject found: Code - {}, Desc - {}. Loading students.", selectedSubjectCode, selectedSubjectDesc);
            subjCodeCombBox.setText(selectedSubjectCode);
            subDesclbl.setText(selectedSubjectDesc);
            loadStudentsBySubjectCode(selectedSubjectCode);
        } else {
            logger.debug("No pre-selected subject. Waiting for user selection.");
        }
        logger.info("EditGradesPageController initialized successfully.");
    }

    // Add method to format grade input
    private String formatGradeInput(String input) {
        logger.trace("Formatting grade input: {}", input);
        try {
            float gradeValue = Float.parseFloat(input);
            String formatted = gradeFormat.format(gradeValue);
            logger.trace("Formatted grade output: {}", formatted);
            return formatted;
        } catch (NumberFormatException e) {
            logger.warn("NumberFormatException while formatting grade input: {}, returning original.", input, e);
            return input;
        }
    }

    private void setupRowHoverEffect() {
        logger.debug("Setting up row hover effect.");
        studentsTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<Student>() {
                @Override
                protected void updateItem(Student item, boolean empty) {
                    super.updateItem(item, empty);
                    // Reset style for empty rows
                    if (empty || item == null) {
                        getStyleClass().add("empty-row"); // Reset style for empty rows
                    } else {
                        getStyleClass().remove("empty-row"); // Reset style for non-empty rows
                    }
                }
            };

            // Add mouse hover effect
            row.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered && !row.isEmpty()) {
                    row.setStyle("table-row-cell:hover"); // Set your desired hover color
                } else {
                    row.setStyle(""); // Reset style when not hovered
                }
            });

            return row;
        });
    }

    private boolean isValidGrade(String grade) {
        logger.trace("Validating grade string: '{}'", grade);
        if (grade == null || grade.trim().isEmpty()) {
            logger.warn("Grade validation failed: Input is null or empty.");
            return false;
        }
        try {
            float gradeValue = Float.parseFloat(grade);
            return gradeValue >= 1.0 && gradeValue <= 5.0;
        } catch (NumberFormatException e) {
            logger.warn("Grade validation failed: '{}' is not a valid number.", grade, e);
            return false;
        }
    }

    private void updateGradeInDatabase(Student student) {
        logger.info("Updating grade in database for student: {} (ID: {}), New Grade: {}", student.getStudentFullName(), student.getStudentNo(), student.getFinalGrade());

        Integer subjectId = getSubjectIdByCode(student.getSubjectCodeForGrade());
        if (subjectId == null) {
            logger.error("Could not find subject ID for {}", student.getSubjectCodeForGrade());
            // Potentially revert the change in the TableView or reload data
            loadStudentsBySubjectCode(selectedSubjectCode); // Reload to revert
            return;
        }

        Float gradeValue = null;
        try {
            if (student.getFinalGrade() != null && !student.getFinalGrade().trim().isEmpty()) {
                 gradeValue = Float.parseFloat(student.getFinalGrade());
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid grade format: {}", student.getFinalGrade(), e);
            loadStudentsBySubjectCode(selectedSubjectCode); // Reload to revert
            return;
        }

        Integer gradeStatusId;
        if (gradeValue != null) {
            gradeStatusId = getGradeStatusId(gradeValue);
            if (gradeStatusId == null) {
                logger.error("Could not determine grade status ID for grade {}", gradeValue);
                loadStudentsBySubjectCode(selectedSubjectCode); // Reload to revert
                return;
            }
        } else {
            gradeStatusId = null;
        }

        // If gradeValue is null, it means the grade is being cleared.
        // In this case, grade_status_id should also likely be null or a specific 'Not Graded' status.
        // For simplicity, if gradeValue is null, we'll set grade_status_id to null.

        String sql = "UPDATE grade SET grade_value = ?, grade_status_id = ? " +
                     "WHERE student_load_id = ? AND subject_id = ?";

        Float finalGradeValue = gradeValue;
        Task<Void> updateTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    if (finalGradeValue != null) {
                        pstmt.setFloat(1, finalGradeValue);
                    } else {
                        pstmt.setNull(1, Types.FLOAT);
                    }
                    
                    if (gradeStatusId != null) {
                        pstmt.setInt(2, gradeStatusId);
                    } else {
                        pstmt.setNull(2, Types.INTEGER);
                    }
                    
                    pstmt.setInt(3, student.getStudentLoadId());
                    pstmt.setInt(4, subjectId);

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows == 0) {
                        // This might happen if the grade record didn't exist, which implies an issue
                        // or if the student is not enrolled in the subject (grades table might not have a row yet).
                        // For an edit grades page, a row should typically exist.
                        // Consider if an INSERT is needed if update fails (upsert logic), though less common for 'edit'.
                        logger.error("No rows updated for student {}, subject {}. Grade record might not exist.", student.getStudentId(), subjectId);
                        // Optionally, attempt to insert if that's the desired behavior for new grades.
                        // For now, we assume the record must exist for an update.
                        Platform.runLater(() -> {
                            showError("Update Failed", "Grade record not found or not updated. Please ensure the student is enrolled.");
                            loadStudentsBySubjectCode(selectedSubjectCode); // Refresh data
                        });
                    } else {
                         // Update the student object's gradeStatusName for immediate UI reflection if not already done by binding
                        Platform.runLater(() -> {
                            if (gradeStatusId != null && finalGradeValue != null) { // only if grade was set
                                String newStatusName = getGradeStatusNameById(gradeStatusId); // Helper needed
                                student.setGradeStatusName(newStatusName != null ? newStatusName : "");
                            } else {
                                student.setGradeStatusName(""); // Cleared grade
                            }
                            // The table should refresh due to ObservableList nature, but sometimes a specific refresh is good.
                            // studentsTable.refresh(); // If direct model update doesn't propagate well
                        });
                    }
                } catch (SQLException e) {
                    logger.error("Database Error: Failed to update grade: {}", e.getMessage(), e);
                    Platform.runLater(() -> {
                        showError("Database Error", "Failed to update grade: " + e.getMessage());
                        loadStudentsBySubjectCode(selectedSubjectCode); // Revert UI on error
                    });
                    throw e;
                }
                return null;
            }
        };
        
        updateTask.setOnFailed(event -> {
            // Error already handled by Platform.runLater in task
             logger.error("Task to update grade failed.");
        });

        new Thread(updateTask).start();
    }

    private Integer getSubjectIdByCode(String subjectCode) {
        String sql = "SELECT subject_id FROM subjects WHERE subject_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subjectCode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("subject_id");
            }
        } catch (SQLException e) {
            logger.error("Database Error: Failed to get subject ID for {}: {}", subjectCode, e.getMessage(), e);
            Platform.runLater(() -> showError("DB Error", "Failed to get subject ID for " + subjectCode));
        }
        return null;
    }

    private Integer getGradeStatusId(double gradeValue) {
        // Determine status name based on grade value
        String statusName;
        if (gradeValue >= 1.0 && gradeValue <= 3.0) {
            statusName = "Passed";
        } else if (gradeValue == 5.0) {
            statusName = "Failed";
        // Add other conditions for INC, DRP, etc. if applicable and present in grade_statuses table
        } else if (gradeValue > 3.0 && gradeValue < 5.0) { // e.g. 4.0 could be conditional/failed
            statusName = "Failed"; // Or a specific status like 'Conditional'
        } else {
             // For grades outside 1.0-3.0 and not 5.0 (e.g. 0 or invalid values if not caught before)
            logger.error("Cannot determine grade status for value: {}", gradeValue);
            return null; // Or a default/error status ID
        }

        String sql = "SELECT grade_status_id FROM grade_statuses WHERE status_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, statusName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("grade_status_id");
            } else {
                logger.error("Grade status ID not found in DB for status: {}", statusName);
                Platform.runLater(() -> showError("DB Error", "Grade status '" + statusName + "' not found."));
            }
        } catch (SQLException e) {
            logger.error("Database Error: Failed to get grade status ID for {}: {}", statusName, e.getMessage(), e);
            Platform.runLater(() -> showError("DB Error", "Failed to get grade status ID for " + statusName));
        }
        return null;
    }
    
    private String getGradeStatusNameById(int gradeStatusId) {
        String sql = "SELECT status_name FROM grade_statuses WHERE grade_status_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gradeStatusId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status_name");
            }
        } catch (SQLException e) {
            logger.error("Database Error: Failed to get grade status name for ID: {}", gradeStatusId, e.getMessage(), e);
            Platform.runLater(() -> showError("DB Error", "Failed to get grade status name for ID: " + gradeStatusId));
        }
        return null;
    }

    // Method to populate subject codes in the dropdown
    private void populateSubjectCodes() {
        logger.info("Populating subject codes dropdown...");
        subjCodeCombBox.getItems().clear(); // Clear existing items
        String sql = "SELECT subject_code, description FROM subjects ORDER BY subject_code"; // Corrected column name

        Task<ObservableList<MenuItem>> task = new Task<>() {
            @Override
            protected ObservableList<MenuItem> call() throws Exception {
                ObservableList<MenuItem> subjectItems = FXCollections.observableArrayList();
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        String code = rs.getString("subject_code");
                        String description = rs.getString("description"); // Corrected column name
                        MenuItem item = new MenuItem(code);
                        item.setUserData(description); // Store description for later use
                        item.setOnAction(event -> handleSubjectCodeSelection(code, description));
                        subjectItems.add(item);
                    }
                } catch (SQLException e) {
                    logger.error("Database Error: Failed to load subject codes: {}", e.getMessage(), e);
                    Platform.runLater(() -> showError("Database Error", "Failed to load subject codes."));
                    throw e; // Re-throw to be caught by task's exception handling
                }
                return subjectItems;
            }
        };

        task.setOnSucceeded(event -> subjCodeCombBox.getItems().addAll(task.getValue()));
        task.setOnFailed(event -> {
            // Error already shown via Platform.runLater in the task
            logger.error("Task to populate subject codes failed.");
        });

        new Thread(task).start();
    }

    // Handle subject code selection
    private void handleSubjectCodeSelection(String subjectCode, String subjectDescription) {
        selectedSubjectCode = subjectCode;
        selectedSubjectDesc = subjectDescription;
        subjCodeCombBox.setText(subjectCode); // Update button text
        subDesclbl.setText(subjectDescription); // Update description label
        subjDescLbl.setText(subjectDescription); // Also update this label if it's different
        gradesHeaderlbl.setText("Grades for " + subjectCode);
        loadStudentsBySubjectCode(subjectCode);
    }

    // Load students by subject code
    private void loadStudentsBySubjectCode(String subjectCode) {
        logger.info("Loading students for subject code: {}", subjectCode);
        studentsList.clear();
        // studentCache.clear(); // Temporarily commented out

        Task<ObservableList<Student>> loadStudentsTask = new Task<>() {
            @Override
            protected ObservableList<Student> call() throws Exception {
                ObservableList<Student> loadedStudents = FXCollections.observableArrayList();
                
                // Corrected SQL Query:
                // Joins students -> student_load -> faculty_load -> subjects to filter by subject_code.
                // Then, student_load -> grade to get the grade.
                // Assumes the current faculty context might be implicitly handled or added later if needed (e.g., by filtering fl.faculty_id).
                String sql = "SELECT s.student_number, s.firstname, s.middlename, s.lastname, " +
                             "g.final_grade AS grade_value, gs.status_name AS grade_status_name, sl.load_id AS student_load_id " +
                             "FROM students s " +
                             "JOIN student_load sl ON s.student_id = sl.student_pk_id " +
                             "JOIN faculty_load fl ON sl.faculty_load = fl.load_id " +
                             "JOIN subjects subj ON fl.subject_id = subj.subject_id " +
                             "LEFT JOIN grade g ON g.student_pk_id = s.student_id AND g.faculty_load = fl.load_id " +
                             "LEFT JOIN grade_statuses gs ON g.grade_status_id = gs.grade_status_id " +
                             "WHERE subj.subject_code = ?";

                logger.debug("Executing SQL to load students: {} with subjectCode: {}", sql, subjectCode);

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, subjectCode);
                    // If you need to filter by the specific faculty teaching this subject instance:
                    // String facultyLoggedIn = SessionData.getInstance().getFacultyNumber(); // or getFacultyId()
                    // int facultyId = getFacultyIdByNumber(facultyLoggedIn); // Helper method needed
                    // pstmt.setInt(2, facultyId); // And add "AND fl.faculty_id = ?" to SQL

                    try (ResultSet rs = pstmt.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            String studentNo = rs.getString("student_number");
                            String firstName = rs.getString("firstname");
                            String middleName = rs.getString("middlename");
                            String lastName = rs.getString("lastname");
                            String studentFullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
                            double finalGradeDouble = rs.getDouble("grade_value");
                            String finalGrade = rs.wasNull() ? "NGS" : gradeFormat.format(finalGradeDouble); // NGS = No Grade Submitted
                            String gradeStatus = rs.getString("grade_status_name");
                            if (gradeStatus == null) gradeStatus = "-"; // Default if no status
                            int studentLoadId = rs.getInt("student_load_id");

                            // The 'subjectCode' for the Student model is the one passed to this method.
                            Student student = new Student(studentNo, studentFullName, subjectCode, finalGrade, gradeStatus, studentLoadId);
                            loadedStudents.add(student);
                            // studentCache.addStudent(student); // Temporarily commented out
                            logger.trace("Loaded student: {} - {}, Grade: {}, Status: {}", studentNo, studentFullName, finalGrade, gradeStatus);
                        }
                        logger.info("Found {} students for subject code: {}", count, subjectCode);
                    }
                } catch (SQLException e) {
                    logger.error("SQL Error loading students for subject {}: ", subjectCode, e);
                    // Ensure the original exception is re-thrown so setOnFailed can see it.
                    // Platform.runLater(() -> showError("Database Error", "Failed to load student data: " + e.getMessage()));
                    throw e;
                }
                return loadedStudents;
            }
        };

        loadStudentsTask.setOnSucceeded(event -> {
            studentsList.setAll(loadStudentsTask.getValue());
            studentsTable.setItems(studentsList);
            logger.debug("Successfully loaded {} students into table for subject: {}", studentsList.size(), subjectCode);
            setupSearchFunctionality(); // Setup search after data is loaded
        });

        loadStudentsTask.setOnFailed(event -> {
            Throwable ex = loadStudentsTask.getException();
            logger.error("Failed to load students for subject {}: ", subjectCode, ex);
            // The showError is now more robust as it's called from the task's exception handler
            showError("Load Error", "Failed to load students. " + ex.getMessage());
        });

        new Thread(loadStudentsTask).start();
    }


    private void setupSearch() {

        // Create a filtered list wrapping the original list
        FilteredList<Student> filteredData = new FilteredList<>(studentsList, p -> true);

        // Add listener to searchBar text property
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(subject -> {
                // If search text is empty, display all subjects
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Convert search text to lower case
                String lowerCaseFilter = newValue.toLowerCase();

                // Match against all fields
                if (subject.getStudentNo().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (subject.getStudentFullName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return subject.getSubjectCodeForGrade().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // Wrap the FilteredList in a SortedList
        SortedList<Student> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(studentsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        studentsTable.setItems(sortedData);

        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
    }

    public void setSubjectCode(String subjectCode) {
        this.selectedSubjectCode = subjectCode;
        if (subjCodeCombBox != null) {
            subjCodeCombBox.setText(subjectCode);
            loadStudentsBySubjectCode(subjectCode);
        }
    }

    private void setupSearchFunctionality() {
        // Create a filtered list wrapping the original list
        FilteredList<Student> filteredData = new FilteredList<>(studentsList, p -> true);

        // Add listener to searchBar text property
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(subject -> {
                // If search text is empty, display all subjects
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Convert search text to lower case
                String lowerCaseFilter = newValue.toLowerCase();

                // Match against all fields
                if (subject.getStudentNo().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (subject.getStudentFullName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return subject.getSubjectCodeForGrade().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // Wrap the FilteredList in a SortedList
        SortedList<Student> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(studentsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        studentsTable.setItems(sortedData);

        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
    }

    public void setSubjectDesc(String subjectDesc) {
        this.selectedSubjectDesc = subjectDesc;
        if (subjDescLbl != null) {
            subjDescLbl.setText(subjectDesc);
        }
    }

    public void clearCacheForSubject(String subjectCode) {
        // studentCache.remove(subjectCode);
    }

    @FXML
    private void handleRefresh() {
        if (selectedSubjectCode != null) {
            // studentCache.remove(selectedSubjectCode);
            loadStudentsBySubjectCode(selectedSubjectCode);
        }
    }

    private void showError(String title, String content) {
        logger.error("Displaying error: Title - '{}', Message - '{}'", title, content);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}