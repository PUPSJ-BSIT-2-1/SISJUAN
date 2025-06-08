package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Student;
import com.example.pupsis_main_dashboard.utilities.*;
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
import javafx.scene.input.KeyCode;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;

import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FacultyEditGradesPageController implements Initializable {

    private static final Logger logger = Logger.getLogger(FacultyEditGradesPageController.class.getName());

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
    @FXML private TableColumn<Student, String> finGradeCol;
    @FXML private TableColumn<Student, String> gradeStatCol;

    private final ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private final StudentCache studentCache = StudentCache.getInstance();
    private String selectedSubjectCode;
    private String selectedSubjectDesc;

    // Add DecimalFormat for consistent grade formatting
    private final DecimalFormat gradeFormat = new DecimalFormat("0.00");

    // Add this field at the top of your class with other fields
    private String selectedYearSection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("FacultyEditGradesPageController initializing...");
        // Add this check at the beginning of initializing
        if (gradesHeaderLbl == null) {
            logger.severe("Error: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
            System.err.println("Error: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
        }

        if (subjCodeCombBox == null) {
            logger.severe("Error: subjCodeCombBox is null. Check FXML file for proper fx:id.");
            System.err.println("Error: subjCodeCombBox is null. Check FXML file for proper fx:id.");
            return;
        }

        if (studentsTable == null) {
            logger.severe("Error: studentsTable is null. Check FXML file for proper fx:id.");
            System.err.println("Error: studentsTable is null. Check FXML file for proper fx:id.");
            return;
        }

        setupRowHoverEffect();

        // Make table editable
        studentsTable.setEditable(true);

        // Initialize the columns with the correct property names
        noStudCol.setCellValueFactory(new PropertyValueFactory<>("studentNo"));
        studIDCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        studNameCol.setCellValueFactory(new PropertyValueFactory<>("studentNa"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjCode"));
        finGradeCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        gradeStatCol.setCellValueFactory(new PropertyValueFactory<>("gradeStatus"));

        gradeStatCol.setCellFactory(tc -> new TableCell<Student, String>() {
            private final Label label = new Label();

            {
                label.setPrefHeight(25);
                label.setMinWidth(Region.USE_PREF_SIZE);
                label.setMaxWidth(Region.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(String gradeStatus, boolean empty) {
                super.updateItem(gradeStatus, empty);

                // Clear all previous style classes
                label.getStyleClass().removeAll("grade-status-passed", "grade-status-failed", "grade-status-other");

                if (empty || gradeStatus == null || gradeStatus.trim().isEmpty()) {
                    setGraphic(null);
                } else {
                    label.setText(gradeStatus);

                    // Apply CSS class based on grade status
                    switch (gradeStatus.toLowerCase()) {
                        case "passed":
                            label.getStyleClass().add("grade-status-passed");
                            break;
                        case "failed":
                            label.getStyleClass().add("grade-status-failed");
                            break;
                        case "incomplete":
                        case "withdrawn":
                        case "dropped":
                            label.getStyleClass().add("grade-status-other");
                            break;
                        default:
                            // No special styling for unknown statuses
                            break;
                    }

                    setGraphic(label);
                }
            }
        });

        // Make final grade column editable with custom cell factory
        finGradeCol.setCellFactory(tc -> new TableCell<Student, String>() {
            private TextField textField;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatGradeForDisplay(item));
                    setGraphic(null);
                }
            }

            @Override
            public void startEdit() {
                super.startEdit();

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
        });

        // Handle the commit of edits
        finGradeCol.setOnEditCommit(event -> {
            Student student = event.getRowValue();
            student.setFinalGrade(event.getNewValue());
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

        // Populate the subject codes dropdown
        populateSubjectCodes();

        // Add this line in the initialize() method after populateSubjectCodes();
        populateYearSections();

        // If a subject code was set before initialization, load it now
        if (selectedSubjectCode != null && selectedSubjectDesc != null) {
            subjCodeCombBox.setText(selectedSubjectCode);
            subDesclbl.setText(selectedSubjectDesc);
            loadStudentsBySubjectCode(selectedSubjectCode);
        }
        logger.info("FacultyEditGradesPageController initialized.");
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

            // NEW RULE: If grade is below 3.01, automatically set to 5.0 (failing)
            if (gradeValue > 3.0f) {
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

    private void setupRowHoverEffect() {

        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
        studentsTable.getColumns().forEach(column -> column.setSortable(false));
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
                "• Numeric grade between 1.00 and 3.00 (grades above 3.00 will be set to 5.00)\n" +
                "• 'W' for withdrawn\n" +
                "• 'D' for dropped";
    }

    // Updated method to handle grade status determination
    private String determineGradeStatus(String finalGrade) {
        if (finalGrade == null || finalGrade.trim().isEmpty()) {
            return "Incomplete";
        }

        String trimmedGrade = finalGrade.trim().toUpperCase();

        // Handle special cases first
        if (trimmedGrade.equals("W")) {
            return "Withdrawn";
        }
        if (trimmedGrade.equals("D")) {
            return "Dropped";
        }

        // Handle numeric grades
        try {
            float gradeValue = Float.parseFloat(trimmedGrade);

            if (gradeValue == 0.0f) { // 0.0 is Incomplete
                return "Incomplete";
            }

            // Per user: 3.0 is Passed.
            // Grades <3.0 (and >=1.0) or >3.0 (and <=5.0) are Failed.
            if (gradeValue >= 1.0f && gradeValue <= 3.0f) {
                return "Passed";
            } else if (gradeValue > 3.0f && gradeValue <= 5.0f) {
                return "Failed";
            } else {
                // For numeric grades outside the 1.0-5.0 range
                return "Incomplete";
            }
        } catch (NumberFormatException e) {
            // If it's not W, D, or a parseable number.
            return "Incomplete";
        }
    }

    private void updateGradeInDatabase(Student student) {
        logger.info("Updating grade for grade_id: " + student.getStudentNo() +
                ", Student Number: " + student.getStudentId() +
                ", New Grade: " + student.getFinalGrade() + ", Status: " + student.getGradeStatus());
        try (Connection conn = DBConnection.getConnection()) {
            // First, determine the grade_status_id based on the final grade
            String newGradeStatus = determineGradeStatus(student.getFinalGrade());
            int gradeStatusId = getGradeStatusId(newGradeStatus, conn);

            // Update both final grade and grade_status_id using grade_id
            String query = "UPDATE grade SET final_grade = ?, grade_status_id = ? WHERE grade_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, student.getFinalGrade());
                pstmt.setInt(2, gradeStatusId);  // Use the actual ID from grade_status table
                pstmt.setInt(3, Integer.parseInt(student.getStudentNo()));

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Update the student object's grade status with the text value
                    student.setGradeStatus(newGradeStatus);

                    // Refresh the specific row in the table
                    int rowIndex = studentsList.indexOf(student);
                    if (rowIndex >= 0) {
                        studentsList.set(rowIndex, student);
                    }

                    // Refresh the TableView
                    studentsTable.refresh();

                    // Show a success message with the formatted grade
                    showSuccess("Success", String.format(
                            "Grade successfully updated to: %s\nStatus: %s",
                            formatGradeForDisplay(student.getFinalGrade()), student.getGradeStatus()));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating grade in database for grade_id: " + student.getStudentNo(), e);
            showError("Database Error", "Failed to update grade: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Error parsing grade_id: " + student.getStudentNo(), e);
            showError("Data Error", "Invalid grade ID format: " + student.getStudentNo());
        }
    }

    private void loadStudentsBySubjectCode(String subjectCode) {
        logger.info("Loading students for Subject Code: " + subjectCode);
        Task<ObservableList<Student>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Student> call() throws Exception {
                ObservableList<Student> tempList = FXCollections.observableArrayList();
                int rowNum = 1; // Initialize counter for auto-incrementing numbers

                try (Connection conn = DBConnection.getConnection()) {
                    StringBuilder queryBuilder = new StringBuilder();
                    queryBuilder.append("""
                SELECT DISTINCT
                       g.grade_id as id,
                       s.student_id,
                       su.subject_code,
                       g.final_grade,
                       gs.status_name as grade_status,
                       s.firstname,
                       s.lastname,
                       CONCAT(s.lastname, ', ', s.firstname, ' ', s.middlename) AS "Student Name",
                       ys.year_section,  
                       f.load_id
                FROM faculty_load f
                JOIN subjects su ON f.subject_id = su.subject_id
                JOIN year_section ys ON f.section_id = ys.section_id 
                JOIN grade g ON g.faculty_load = f.load_id
                JOIN students s ON g.student_pk_id = s.student_id
                LEFT JOIN grade_statuses gs ON g.grade_status_id = gs.grade_status_id
                WHERE su.subject_code = ?
                AND CAST(f.faculty_id AS TEXT) = ?
                """);

                    if (selectedYearSection != null && !selectedYearSection.equals("All")) {
                        queryBuilder.append(" AND CAST(ys.year_section AS TEXT) = ?");
                    }
                    queryBuilder.append(" ORDER BY s.lastname, s.firstname");

                    try (PreparedStatement pstmt = conn.prepareStatement(
                            queryBuilder.toString(),
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY)) {

                        pstmt.setFetchSize(100);
                        pstmt.setString(1, subjectCode);
                        pstmt.setString(2, String.valueOf(SessionData.getInstance().getFacultyId()));

                        if (selectedYearSection != null && !selectedYearSection.equals("All")) {
                            pstmt.setString(3, selectedYearSection);
                        }

                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next() && !isCancelled()) {
                                String finalGradeFromDB = rs.getString("final_grade");
                                String formattedGrade = formatGradeFromDB(finalGradeFromDB);

                                Student student = new Student(
                                        String.valueOf(rowNum++), // Use auto-incrementing number instead of grade_id
                                        rs.getString("student_id"),
                                        rs.getString("Student Name"),
                                        rs.getString("subject_code"),
                                        formattedGrade != null ? formattedGrade : "",
                                        rs.getString("grade_status") != null ? rs.getString("grade_status") : ""
                                );
                                tempList.add(student);
                            }
                        }
                    }
                    return tempList;
                }
            }
    };

    loadTask.setOnSucceeded(e -> {
        ObservableList<Student> result = loadTask.getValue();
        studentCache.put(subjectCode, result);
        updateTableView(result);
        setupSearch();

        Platform.runLater(() -> {
            if (gradesHeaderLbl != null) {
                String headerText = selectedYearSection != null && !selectedYearSection.equals("All")
                        ? String.format("%s - %s", subjectCode, selectedYearSection)
                        : subjectCode;
            } else {
                logger.severe("Warning: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
                System.err.println("Warning: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
            }
        });
    });

    loadTask.setOnFailed(e -> {
        Throwable ex = loadTask.getException();
        logger.log(Level.SEVERE, "Error loading students by subject code: " + subjectCode, ex);
        showError("Database Error", "Failed to load data: " + ex.getMessage());
    });

    new Thread(loadTask).start();
}
    private int getGradeStatusId(String statusName, Connection conn) throws SQLException {
        String query = "SELECT grade_status_id FROM grade_statuses WHERE status_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, statusName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("grade_status_id");
                } else {
                    // If status not found, you might want to create it or use a default
                    throw new SQLException("Grade status '" + statusName + "' not found in database");
                }
            }
        }
    }

    private void populateSubjectCodes() {
        logger.info("Populating subject codes for faculty ID: " + SessionData.getInstance().getFacultyId());
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
            SELECT DISTINCT s.subject_code, s.description
            FROM faculty_load f
            JOIN subjects s ON f.subject_id = s.subject_id
            WHERE CAST(f.faculty_id AS TEXT) = ? 
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
                        logger.info("Subject code selected: " + subjCode + " (" + subjDesc + ")");
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
            logger.log(Level.SEVERE, "Error loading subject codes for faculty ID: " + SessionData.getInstance().getFacultyId(), e);
            showError("Database Error", "Failed to load subject codes: " + e.getMessage());
        }
    }

    private void updateTableView(ObservableList<Student> students) {
        Platform.runLater(() -> {
            studentsList.clear();
            studentsList.addAll(students);
            studentsTable.setItems(studentsList);
        });
    }

    public void setSubjectCode(String subjectCode) {
        this.selectedSubjectCode = subjectCode;
        if (subjCodeCombBox != null) {
            subjCodeCombBox.setText(subjectCode);
            // Populate year sections for this specific subject
            populateYearSectionsForSubject(subjectCode);
            loadStudentsBySubjectCode(subjectCode);
        }
    }

    private void setupSearch() {
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
                if (subject.getStudentId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (subject.getStudentNa().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return subject.getSubjCode().toLowerCase().contains(lowerCaseFilter);
            });

            // Update row numbers for filtered results
            int rowNum = 1;
            for (Student student : filteredData) {
                student.setStudentNo(String.valueOf(rowNum++));
            }
        });

        // Wrap the FilteredList in a SortedList
        SortedList<Student> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(studentsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        studentsTable.setItems(sortedData);

        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
    }

    private void populateYearSectionsForSubject(String subjectCode) {
        logger.info("Populating year sections for subject: " + subjectCode);
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
        SELECT DISTINCT ys.year_section 
        FROM faculty_load f
        JOIN subjects s ON f.subject_id = s.subject_id
        JOIN year_section ys ON f.section_id = ys.section_id 
        WHERE f.faculty_id = CAST(? AS SMALLINT) 
        AND s.subject_code = ?
        ORDER BY ys.year_section 
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
                            logger.info("Year section selected: " + yearSection);
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
            logger.log(Level.SEVERE, "Error loading year sections for subject: " + subjectCode, e);
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
        studentCache.remove(subjectCode);
    }

    @FXML
    private void handleRefresh() {
        logger.info("Refreshing students for subject code: " + selectedSubjectCode);
        if (selectedSubjectCode != null) {
            studentCache.remove(selectedSubjectCode);
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
        logger.info("Showing error dialog: " + title + " - " + content);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccess(String title, String content) {
        logger.info("Showing success dialog: " + title + " - " + content);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Modify the populateYearSections method to include filtering
    private void populateYearSections() {
        logger.info("Populating year sections for faculty ID: " + SessionData.getInstance().getFacultyId());
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
        SELECT DISTINCT ys.year_section 
        FROM faculty_load fl
        JOIN year_section ys ON fl.section_id = ys.section_id
        WHERE fl.faculty_id = CAST(? AS SMALLINT) 
        ORDER BY ys.year_section;
        """;

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, SessionData.getInstance().getFacultyId());
                ResultSet rs = pstmt.executeQuery();

                yrSecCombBox.getItems().clear();

                // Add "All" option for initial load
                MenuItem allItem = new MenuItem("All");
                allItem.setOnAction(event -> {
                    logger.info("Year section selected: All");
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
                        logger.info("Year section selected: " + yearSection);
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
            logger.log(Level.SEVERE, "Error loading year sections: " + e.getMessage(), e);
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