package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Student;
import com.example.pupsis_main_dashboard.utilities.DBConnection;import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.util.converter.DoubleStringConverter;
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
    @FXML private Label gradesHeaderLbl;
    @FXML private Label subDesclbl;
    @FXML private Label subjDescLbl;
    @FXML private MenuButton subjCodeCombBox;
    @FXML private MenuButton yrSecCombBox;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> noStudCol;
    @FXML private TableColumn<Student, String> studIDCol;
    @FXML private TableColumn<Student, String> studNameCol;
    @FXML private TableColumn<Student, String> subjCodeCol;
    @FXML private TableColumn<Student, Double> finGradeCol; // Changed to Double
    @FXML private TableColumn<Student, String> gradeStatCol;

    private final ObservableList<Student> studentsList = FXCollections.observableArrayList();
    // private final StudentCache studentCache = StudentCache.getInstance(); // Temporarily commented out
    private String selectedSubjectCode;
    private String selectedSubjectDesc;

    // Add DecimalFormat for consistent grade formatting
    private final DecimalFormat gradeFormat = new DecimalFormat("0.00");

    // Add this field at the top of your class with other fields
    private String selectedYearSection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing EditGradesPageController...");
        // Add this check at the beginning of initializing
        if (gradesHeaderLbl == null) {
            System.err.println("Error: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
        }

       if (subjCodeCombBox == null) {
           System.err.println("Error: subjCodeCombBox is null. Check FXML file for proper fx:id.");
           return;
       }

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
        finGradeCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade")); // Changed to Double
        gradeStatCol.setCellValueFactory(new PropertyValueFactory<>("gradeStatusName"));
        logger.debug("Table columns initialized with PropertyValueFactory.");

        // Make final grade column editable with custom cell factory
        finGradeCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter() {
            @Override
            public String toString(Double object) {
                if (object == null) {
                    return "NGS"; // Display NGS if grade is null
            
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatGradeForDisplay(item));
                    setGraphic(null);
                }
                return gradeFormat.format(object); // Format to two decimal places
            }

            @Override
            public Double fromString(String string) {
                if (string == null || string.trim().isEmpty() || "NGS".equalsIgnoreCase(string.trim())) {
                    return null; // Allow clearing the grade or entering NGS
                }
                try {
                    // Attempt to parse, if it fails, super.fromString will throw NumberFormatException
                    double val = Double.parseDouble(string);
                    if (val < 1.0 || (val > 3.0 && val < 5.0) || val > 5.0) { // Basic validation for typical 1.0-3.0, 5.0 scale
                         // Or handle more complex validation if needed, e.g., specific allowed values
                        // For now, let's allow it and let the logic decide status
                    }
                    return val;
                } catch (NumberFormatException e) {
                    // Handle invalid input, e.g., show an error or return a specific value
                    // For now, rethrow or let the TableView handle it by not committing the edit.
                    // Platform.runLater(() -> showError("Invalid Input", "Please enter a valid number for grade or NGS."));
                    // To prevent commit on invalid format, we can throw an exception that the table can catch
                    // or return the old value if we had access to it here.
                    // For simplicity, we'll let it try to parse and potentially fail if not a double.
                    logger.warn("Invalid grade input: {}", string);
                    // Returning null or throwing an exception might be options depending on desired UX
                    // For now, let DoubleStringConverter handle the exception if not parsable.
                    return super.fromString(string); 
                }
                setText(null);
                setGraphic(textField);
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(formatGradeForDisplay(getItem()));
                setGraphic(null);
            }

            private void createTextField() {
                String initialValue = getItem();
                textField = new TextField(initialValue);
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        String inputValue = textField.getText().trim().toUpperCase();
                        if (isValidGrade(inputValue)) {
                            String formattedGrade = formatGradeInput(inputValue);
                            commitEdit(formattedGrade);
                        } else {
                            cancelEdit();
                            showError("Invalid Grade", getGradeValidationMessage());
                        }
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        String inputValue = textField.getText().trim().toUpperCase();
                        if (isValidGrade(inputValue)) {
                            String formattedGrade = formatGradeInput(inputValue);
                            commitEdit(formattedGrade);
                        } else {
                            cancelEdit();
                            showError("Invalid Grade", getGradeValidationMessage());
                        }
                    }
                });
            }
        }));

        finGradeCol.setOnEditCommit(event -> {
            Student student = event.getRowValue();
            Double newValue = event.getNewValue();
            student.setFinalGrade(newValue);
            // Derive and set status immediately
            String derivedStatus;
            if (newValue == null) {
                derivedStatus = "NGS";
            } else if (newValue >= 1.0 && newValue <= 3.0) {
                derivedStatus = "Passed";
            } else if (newValue == 5.0) {
                derivedStatus = "Failed";
            } else {
                derivedStatus = "Inc."; // Or some other status for grades like 4.0 or invalid ones
            }
            student.setGradeStatusName(derivedStatus);
            updateGradeInDatabase(student);
            studentsTable.refresh(); // Refresh to show updated status if necessary
        });

        // Grade Status Column (Display only, derived)
        gradeStatCol.setCellValueFactory(new PropertyValueFactory<>("gradeStatusName"));
        gradeStatCol.setEditable(false); // Status is derived, not directly editable

        // Populate the subject codes dropdown
        populateSubjectCodes();

        // Add this line in the initialize() method after populateSubjectCodes();
        populateYearSections();

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
    }
    private String roundGradeToQuarter(String gradeInput) {
        if (gradeInput == null || gradeInput.trim().isEmpty()) {
            return gradeInput;
        }

        String trimmedInput = gradeInput.trim().toUpperCase();

        // Handle special cases
        if (trimmedInput.equals("W") || trimmedInput.equals("D")) {
            return trimmedInput;
        }

        try {
            float gradeValue = Float.parseFloat(trimmedInput);

            // Handle edge cases
            if (gradeValue <= 0.0) {
                return "0.00";
            }
            if (gradeValue >= 5.0) {
                return "5.00";
            }

            // Round to nearest quarter, but always round up within each quarter range
            // Get the whole number part
            int wholePart = (int) gradeValue;
            float fractionalPart = gradeValue - wholePart;

            float roundedFractional;
            if (fractionalPart > 0.00 && fractionalPart <= 0.25) {
                roundedFractional = 0.25f;
            } else if (fractionalPart > 0.25 && fractionalPart <= 0.50) {
                roundedFractional = 0.50f;
            } else if (fractionalPart > 0.50 && fractionalPart <= 0.75) {
                roundedFractional = 0.75f;
            } else if (fractionalPart > 0.75 && fractionalPart < 1.00) {
                roundedFractional = 1.00f;
                wholePart++; // Carry over to next whole number
                if (wholePart > 5) wholePart = 5; // Cap at 5.00
            } else {
                roundedFractional = 0.00f; // Exact whole numbers stay as is
            }

            float finalGrade = wholePart + (wholePart < 5 ? roundedFractional : 0.00f);
            return gradeFormat.format(finalGrade);

        } catch (NumberFormatException e) {
            return gradeInput;
        }
    }
                                    
    // Updated method to handle both numeric grades and special text values
    private String formatGradeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String trimmedInput = input.trim().toUpperCase();

        // Handle special cases
        if (trimmedInput.equals("W") || trimmedInput.equals("D")) {
            return trimmedInput;
        }

        // Try to parse and round numeric grade
        try {
            float gradeValue = Float.parseFloat(trimmedInput);
            // Apply rounding logic
            return roundGradeToQuarter(trimmedInput);
        } catch (NumberFormatException e) {
            return input;
        }
    }

    // Updated method to format grades for display
    private String formatGradeForDisplay(String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            return "";
        }

        String trimmedGrade = grade.trim().toUpperCase();

        // Handle special cases
        if (trimmedGrade.equals("W") || trimmedGrade.equals("D")) {
            return trimmedGrade;
        }

        // Try to format as numeric grade
        try {
            float gradeValue = Float.parseFloat(trimmedGrade);
            return gradeFormat.format(gradeValue);
        } catch (NumberFormatException e) {
            return grade;
        }
    }
                                    
    private void updateGradeInDatabase(Student student) {
        logger.info("Attempting to update grade for student via StudentLoadID: {} for subject: {}", student.getStudentLoadId(), student.getSubjectCodeForGrade());

        String subjectCode = student.getSubjectCodeForGrade();
        Integer subjectId = getSubjectIdByCode(subjectCode); // Helper method to get subject_id
        if (subjectId == null) { // Check for null as getSubjectIdByCode can return null on error/not found
            logger.error("Cannot update grade. Unknown subject code or DB error for: {}", subjectCode);
            showError("Update Error", "Cannot update grade. Unknown subject code or DB error for: " + subjectCode);
            return;
        }
    private void setupRowHoverEffect() {
        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
    }

    // Updated validation method to handle both numeric grades and special text values
    private boolean isValidGrade(String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            return false;
        }

        String trimmedGrade = grade.trim().toUpperCase();

        // Check for special cases
        if (trimmedGrade.equals("W") || trimmedGrade.equals("D")) {
            return true;
        }

        // Check for numeric grade (including 0 for incomplete)
        try {
            float gradeValue = Float.parseFloat(trimmedGrade);
            return gradeValue >= 0.0 && gradeValue <= 5.0; // Changed from 1.0 to 0.0
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Updated method to provide comprehensive validation message
    private String getGradeValidationMessage() {
        return "Please enter a valid grade:\n" +
                "• Numeric grade between 1.00 and 5.00\n" +
                "• 'W' for withdrawn\n" +
                "• 'D' for dropped";
    }

    // Updated method to handle grade status determination
    private String determineGradeStatus(String finalGrade) {
        if (finalGrade == null || finalGrade.trim().isEmpty()) {
            return "Incomplete";
        }

        String trimmedGrade = finalGrade.trim().toUpperCase();

        // Handle special cases
        if (trimmedGrade.equals("W")) {
            return "Withdrawn";
        }
        if (trimmedGrade.equals("D")) {
            return "Dropped";
        }

        // Handle numeric grades
        try {
            float gradeValue = Float.parseFloat(trimmedGrade);
            if (gradeValue == 0.0) {
                return "Incomplete";  // Handle 0 as incomplete
            } else if (gradeValue >= 1.00 && gradeValue <= 3.00) {
                return "Passed";
            } else if (gradeValue > 3.00 && gradeValue <= 5.00) {
                return "Failed";
            } else {
                return "Incomplete";
            }
        } catch (NumberFormatException e) {
            return "INC";
        }
    }

    private void updateGradeInDatabase(Student student) {
        try (Connection conn = DBConnection.getConnection()) {
            // Update both final grade and grade status
            String query = "UPDATE grade \n" +
                    "SET final_grade = ?, gradestat = ? \n" +
                    "FROM subjects \n" +
                    "WHERE grade.student_id = ? \n" +
                    "AND grade.subject_id = subjects.subject_id \n" +
                    "AND subjects.subject_code = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                String newGrade = student.getFinalGrade();
                String gradeStatus = determineGradeStatus(newGrade);

                // Set parameters for update - store as text since the column is text
                pstmt.setString(1, newGrade);
                pstmt.setString(2, gradeStatus);
                pstmt.setString(3, student.getStudentId());
                pstmt.setString(4, student.getSubjCode());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Update the student object's grade status
                    student.setGradeStatus(gradeStatus);

                    // Refresh the specific row in the table
                    int rowIndex = studentsList.indexOf(student);
                    if (rowIndex >= 0) {
                        studentsList.set(rowIndex, student);
                    }

        Double gradeValue = student.getFinalGrade(); // Get Double grade from Student object

        String checkSql = "SELECT g.grade_id, sl.student_pk_id, sl.faculty_load " +
                        "FROM student_load sl " +
                        "LEFT JOIN grade g ON g.student_pk_id = sl.student_pk_id AND g.faculty_load = sl.faculty_load " +
                        "WHERE sl.load_id = ?";

        final String[] upsertSql = new String[1];

        Task<Void> updateTask = new Task<>() {
                    // Show a success message with the formatted grade
                    showSuccess("Success", String.format(
                            "Grade successfully updated to: %s\nStatus: %s",
                            formatGradeForDisplay(newGrade), gradeStatus));
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update grade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStudentsBySubjectCode(String subjectCode) {
        Task<ObservableList<Student>> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = DBConnection.getConnection()) {
                    Integer gradeIdToUpdate = null;
                    Integer studentPkIdForGrade = null;
                    Integer facultyLoadIdForGrade = null;

                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setInt(1, student.getStudentLoadId());
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            gradeIdToUpdate = rs.getObject("grade_id") != null ? rs.getInt("grade_id") : null;
                            studentPkIdForGrade = rs.getInt("student_pk_id");
                            facultyLoadIdForGrade = rs.getInt("faculty_load");
                        } else {
                            logger.error("Could not find student_load record for sl.load_id: {}", student.getStudentLoadId());
                            throw new SQLException("Student load record not found, cannot update grade.");
                        }
                    }

                    if (studentPkIdForGrade == null || facultyLoadIdForGrade == null) {
                        logger.error("Critical: student_pk_id or faculty_load_id is null from student_load for sl.load_id: {}", student.getStudentLoadId());
                        throw new SQLException("Cannot determine student or faculty load for grade update.");
                    }

                    if (gradeIdToUpdate != null) { // Grade exists, UPDATE it
                        logger.debug("Updating existing grade_id: {} for student_pk_id: {}, faculty_load: {}", gradeIdToUpdate, studentPkIdForGrade, facultyLoadIdForGrade);
                        upsertSql[0] = "UPDATE grade SET final_grade = ?, grade_status_id = NULL " +
                                    "WHERE grade_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(upsertSql[0])) {
                            if (gradeValue != null) {
                                pstmt.setDouble(1, gradeValue);
                            } else {
                                pstmt.setNull(1, Types.DOUBLE);
                              
                    StringBuilder queryBuilder = new StringBuilder();
                    queryBuilder.append("""
                    SELECT DISTINCT 
                           g.grade_id as id, 
                           g.student_id, 
                           su.subject_code, 
                           g.final_grade, 
                           g.gradestat, 
                           s.firstname,
                           s.lastname,
                           CONCAT(s.lastname, ', ', s.firstname, ' ', s.middlename) AS "Student Name",
                           f.year_section,
                           f.load_id
                    FROM faculty_load f
                    JOIN subjects su ON f.subject_id = su.subject_id
                    JOIN grade g ON g.faculty_load = f.load_id
                    JOIN students s ON g.student_id = s.student_number
                    WHERE su.subject_code = ? 
                    AND f.faculty_id = ?::smallint
                """);

                    if (selectedYearSection != null && !selectedYearSection.equals("All")) {
                        queryBuilder.append(" AND f.year_section = ?");
                    }
                    queryBuilder.append(" ORDER BY s.lastname, s.firstname");

                    try (PreparedStatement pstmt = conn.prepareStatement(
                            queryBuilder.toString(),
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY)) {

                        pstmt.setFetchSize(100);
                        pstmt.setString(1, subjectCode);
                        pstmt.setString(2, SessionData.getInstance().getFacultyId());

                        if (selectedYearSection != null && !selectedYearSection.equals("All")) {
                            pstmt.setString(3, selectedYearSection);
                        }

                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next() && !isCancelled()) {
                                String finalGradeFromDB = rs.getString("final_grade");
                                String formattedGrade = formatGradeFromDB(finalGradeFromDB);

                                Student student = new Student(
                                        rs.getString("id"),
                                        rs.getString("student_id"),
                                        rs.getString("Student Name"),
                                        rs.getString("subject_code"),
                                        formattedGrade != null ? formattedGrade : "",
                                        rs.getString("gradestat") != null ? rs.getString("gradestat") : ""
                                );
                                tempList.add(student);
                            }
                            pstmt.setInt(2, gradeIdToUpdate);
                            pstmt.executeUpdate();
                            logger.info("Successfully updated grade for student_pk_id: {}, faculty_load: {}", studentPkIdForGrade, facultyLoadIdForGrade);
                        }
                    } else { // Grade does not exist, INSERT it
                        logger.debug("Inserting new grade for student_pk_id: {}, faculty_load: {}", studentPkIdForGrade, facultyLoadIdForGrade);
                        upsertSql[0] = "INSERT INTO grade (student_pk_id, faculty_load, subject_id, final_grade, grade_status_id, academic_year_id) " +
                                    "VALUES (?, ?, ?, ?, NULL, ?)";
                        
                        int currentAcademicYearId = SessionData.getInstance().getCurrentAcademicYearId(); 

                        try (PreparedStatement pstmt = conn.prepareStatement(upsertSql[0])) {
                            pstmt.setInt(1, studentPkIdForGrade);
                            pstmt.setInt(2, facultyLoadIdForGrade);
                            pstmt.setInt(3, subjectId);
                            if (gradeValue != null) {
                                pstmt.setDouble(4, gradeValue);
                            } else {
                                pstmt.setNull(4, Types.DOUBLE);
                            }
                            pstmt.setInt(5, currentAcademicYearId);
                            pstmt.executeUpdate();
                            logger.info("Successfully inserted grade for student_pk_id: {}, faculty_load: {}", studentPkIdForGrade, facultyLoadIdForGrade);
                        }
                    }
                    return null;
                } catch (SQLException e) {
                    logger.error("Database error during grade update for student via StudentLoadID {}: ", student.getStudentLoadId(), e);
                    throw e; // Re-throw to be caught by setOnFailed
                }
            }
        };

        updateTask.setOnSucceeded(event -> {
            logger.info("Grade update task succeeded for student via StudentLoadID: {}", student.getStudentLoadId());
            // The UI (TableView cell and status) should have been updated by onEditCommit handler already.
            // No need to update student.gradeStatusName here.
            // studentsTable.refresh(); // Consider if refresh is needed for other reasons.
        loadTask.setOnSucceeded(e -> {
            ObservableList<Student> result = loadTask.getValue();
            studentCache.put(subjectCode, result);
            updateTableView(result);
            setupSearch();

            Platform.runLater(() -> {
                // Add null check for gradesHeaderLbl
                if (gradesHeaderLbl != null) {
                    String headerText = selectedYearSection != null && !selectedYearSection.equals("All")
                            ? String.format("%s - %s", subjectCode, selectedYearSection)
                            : subjectCode;
                } else {
                    System.err.println("Warning: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
                }
            });
        });

        updateTask.setOnFailed(event -> {
            Throwable ex = updateTask.getException();
            logger.error("Grade update task failed for student via StudentLoadID: {}. Error: ", student.getStudentLoadId(), ex.getMessage(), ex);
            showError("Update Error", "Failed to update grade: " + ex.getMessage());
            // Consider reloading data or reverting UI changes on critical failure
            // loadStudentsBySubjectCode(this.selectedSubjectCode); 
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
              
    private void populateSubjectCodes() {
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
            SELECT DISTINCT s.subject_code, s.description
            FROM faculty_load f
            JOIN subjects s ON f.subject_id = s.subject_id
            WHERE f.faculty_id = ?::smallint
            ORDER BY s.subject_code;
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, SessionData.getInstance().getFacultyId());
                ResultSet rs = pstmt.executeQuery();

                subjCodeCombBox.getItems().clear();

                while (rs.next()) {
                    String subjCode = rs.getString("subject_code");
                    String subjDesc = rs.getString("description");
                    MenuItem item = new MenuItem(subjCode);
                    item.setOnAction(event -> {
                        subjCodeCombBox.setText(subjCode);
                        subjDescLbl.setText(subjDesc);
                        selectedSubjectCode = subjCode;
                        selectedSubjectDesc = subjDesc;

                        // Populate year sections based on selected subject
                        populateYearSectionsForSubject(subjCode);

                        // Load students with the selected subject and year section
                        loadStudentsBySubjectCode(subjCode);
                    });
                    subjCodeCombBox.getItems().add(item);
                }
            }
        } catch (SQLException e) {
            logger.error("Database Error: Failed to get subject ID for {}: {}", subjectCode, e.getMessage(), e);
            Platform.runLater(() -> showError("DB Error", "Failed to get subject ID for " + subjectCode));
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

    public void setSubjectCode(String subjectCode) {
        this.selectedSubjectCode = subjectCode;
        if (subjCodeCombBox != null) {
            subjCodeCombBox.setText(subjectCode);
            // Populate year sections for this specific subject
            populateYearSectionsForSubject(subjectCode);
            loadStudentsBySubjectCode(subjectCode);
        }

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
                            Double finalGradeDouble = rs.getObject("grade_value") != null ? rs.getDouble("grade_value") : null;
                            String derivedGradeStatusName;
                            if (finalGradeDouble == null) {
                                derivedGradeStatusName = "NGS";
                            } else if (finalGradeDouble >= 1.0 && finalGradeDouble <= 3.0) {
                                derivedGradeStatusName = "Passed";
                            } else if (finalGradeDouble == 5.0) {
                                derivedGradeStatusName = "Failed";
                            } else {
                                // Handle other cases, e.g., grades like 4.0, or out of expected range
                                derivedGradeStatusName = "Inc."; // Or "Invalid", or based on specific rules
                            }

                            int studentLoadId = rs.getInt("student_load_id");

                            Student student = new Student(studentNo, firstName, middleName, lastName, subjectCode, finalGradeDouble, derivedGradeStatusName, studentLoadId);
                            loadedStudents.add(student);
                            logger.trace("Loaded student: {} - {} {} {}, Grade: {}, Status: {}, StudentLoadID: {}", 
                                studentNo, firstName, middleName, lastName, 
                                finalGradeDouble == null ? "NGS" : gradeFormat.format(finalGradeDouble), 
                                derivedGradeStatusName, studentLoadId);
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

    private void setupSearchFunctionality() {
        // Create a filtered list wrapping the original list
        FilteredList<Student> filteredData = new FilteredList<>(studentsList, p -> true);

        // Add listener to searchBar text property
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(subject -> {
                // If a search text is empty, display all subjects
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

    private void populateYearSectionsForSubject(String subjectCode) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
        SELECT DISTINCT f.year_section
        FROM faculty_load f 
        JOIN subjects s ON f.subject_id = s.subject_id
        WHERE f.faculty_id = ?::smallint 
        AND s.subject_code = ?
        ORDER BY f.year_section;
        """;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, SessionData.getInstance().getFacultyId());
                pstmt.setString(2, subjectCode);
                ResultSet rs = pstmt.executeQuery();

                yrSecCombBox.getItems().clear();

                // Collect all year sections in a list first
                java.util.List<String> yearSections = new java.util.ArrayList<>();
                while (rs.next()) {
                    yearSections.add(rs.getString("year_section"));
                }

                // Now process based on the count
                if (yearSections.size() == 1) {
                    // If only one section, disable dropdown and set the section
                    String yearSection = yearSections.get(0);
                    yrSecCombBox.setText(yearSection);
                    yrSecCombBox.setDisable(true);
                    selectedYearSection = yearSection;
                }
                else if (yearSections.size() > 1) {
                    // If multiple sections, enable dropdown and populate with sections
                    yrSecCombBox.setDisable(false);

                    for (String yearSection : yearSections) {
                        MenuItem item = new MenuItem(yearSection);
                        item.setOnAction(event -> {
                            yrSecCombBox.setText(yearSection);
                            selectedYearSection = yearSection;
                            loadStudentsBySubjectCode(selectedSubjectCode);
                        });
                        yrSecCombBox.getItems().add(item);
                    }

                    // Set the first section as default if nothing is selected
                    if (selectedYearSection == null) {
                        String firstSection = yearSections.get(0);
                        yrSecCombBox.setText(firstSection);
                        selectedYearSection = firstSection;
                    }
                }
                else {
                    // If no sections found, disable dropdown
                    yrSecCombBox.setText("No Sections");
                    yrSecCombBox.setDisable(true);
                    selectedYearSection = null;
                }

            }
        } catch (SQLException e) {
            System.err.println("Error loading year sections for subject: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load year sections: " + e.getMessage());

        }
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

    public void setYearSection(String yearSection) {
        this.selectedYearSection = yearSection;
        if (yrSecCombBox != null) {
            if (yearSection == null || yearSection.equals("All")) {
                yrSecCombBox.setText("All");
                selectedYearSection = null;
            } else {
                yrSecCombBox.setText(yearSection);
                selectedYearSection = yearSection;
            }

            // If a subject is already selected, reload the data with the new year section filter
            if (selectedSubjectCode != null) {
                loadStudentsBySubjectCode(selectedSubjectCode);
            }
        }
    }

    public void setSubjectAndYearSection(String subjectCode, String subjectDesc, String yearSection) {
        this.selectedSubjectCode = subjectCode;
        this.selectedSubjectDesc = subjectDesc;
        this.selectedYearSection = yearSection;

        if (subjCodeCombBox != null) {
            subjCodeCombBox.setText(subjectCode);
        }
        if (subjDescLbl != null) {
            subjDescLbl.setText(subjectDesc);
        }

        // Populate year sections for the specific subject first
        populateYearSectionsForSubject(subjectCode);

        // Then set the specific year section if provided
        if (yrSecCombBox != null && yearSection != null && !yearSection.equals("All")) {
            yrSecCombBox.setText(yearSection);
            selectedYearSection = yearSection;
        }

        // Load students with both filters
        loadStudentsBySubjectCode(subjectCode);
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

    // Modify the populateYearSections method to include filtering
    private void populateYearSections() {
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
        SELECT DISTINCT f.year_section 
        FROM faculty_load f 
        WHERE f.faculty_id = ?::smallint 
        ORDER BY f.year_section;
        """;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, SessionData.getInstance().getFacultyId());
                ResultSet rs = pstmt.executeQuery();

                yrSecCombBox.getItems().clear();

                // Add "All" option for initial load
                MenuItem allItem = new MenuItem("All");
                allItem.setOnAction(event -> {
                    yrSecCombBox.setText("All");
                    selectedYearSection = null;
                    if (selectedSubjectCode != null) {
                        loadStudentsBySubjectCode(selectedSubjectCode);
                    }
                });
                yrSecCombBox.getItems().add(allItem);

                while (rs.next()) {
                    String yearSection = rs.getString("year_section");
                    MenuItem item = new MenuItem(yearSection);
                    item.setOnAction(event -> {
                        yrSecCombBox.setText(yearSection);
                        selectedYearSection = yearSection;
                        if (selectedSubjectCode != null) {
                            loadStudentsBySubjectCode(selectedSubjectCode);
                        }
                    });
                    yrSecCombBox.getItems().add(item);
                }

                // Enable dropdown initially
                yrSecCombBox.setDisable(false);
            }
        } catch (SQLException e) {
            System.err.println("Error loading year sections: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load year sections: " + e.getMessage());
        }
    }

    // Updated helper method to format grades from database
    private String formatGradeFromDB(String gradeStr) {
        if (gradeStr == null || gradeStr.trim().isEmpty()) {
            return gradeStr;
        }

        String trimmedGrade = gradeStr.trim().toUpperCase();

        // Handle special cases
        if (trimmedGrade.equals("W") || trimmedGrade.equals("D")) {
            return trimmedGrade;
        }

        // Try to format as numeric grade (including 0)
        try {
            float gradeValue = Float.parseFloat(trimmedGrade);
            return gradeFormat.format(gradeValue);
        } catch (NumberFormatException e) {
            return gradeStr;
        }
    }
}