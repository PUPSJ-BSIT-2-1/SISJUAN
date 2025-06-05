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

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.text.DecimalFormat;

public class EditGradesPageController implements Initializable {

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
        if (studentsTable == null) {
            System.err.println("Error: studentsTable is null. Check FXML file for proper fx:id.");
            return;
        }

        setupRowHoverEffect();

        // Make table editable
        studentsTable.setEditable(true);

        // Initialize the columns with the correct property names
        noStudCol.setCellValueFactory(new PropertyValueFactory<>("studentNo")); // Student's official number
        studIDCol.setCellValueFactory(new PropertyValueFactory<>("studentNo")); // Displaying studentNo as ID as well
        studNameCol.setCellValueFactory(new PropertyValueFactory<>("studentFullName"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCodeForGrade"));
        finGradeCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        gradeStatCol.setCellValueFactory(new PropertyValueFactory<>("gradeStatusName"));

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
                    // Format the grade for display
                    try {
                        float gradeValue = Float.parseFloat(item);
                        setText(gradeFormat.format(gradeValue));
                    } catch (NumberFormatException e) {
                        setText(item); // fallback to original if parsing fails
                    }
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
                // Format the grade when canceling edit
                try {
                    float gradeValue = Float.parseFloat(getItem());
                    setText(gradeFormat.format(gradeValue));
                } catch (NumberFormatException e) {
                    setText(getItem());
                }
                setGraphic(null);
            }

            private void createTextField() {
                // Set initial value with proper formatting
                String initialValue = getItem();
                try {
                    float gradeValue = Float.parseFloat(initialValue);
                    initialValue = gradeFormat.format(gradeValue);
                } catch (NumberFormatException e) {
                    // keep original value if parsing fails
                }

                textField = new TextField(initialValue);
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        if (isValidGrade(textField.getText())) {
                            // Format the input before committing
                            String formattedGrade = formatGradeInput(textField.getText());
                            commitEdit(formattedGrade);
                        } else {
                            cancelEdit();
                            showError("Invalid Grade", "Please enter a valid grade between 1.00 and 5.00");
                        }
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        if (isValidGrade(textField.getText())) {
                            // Format the input before committing
                            String formattedGrade = formatGradeInput(textField.getText());
                            commitEdit(formattedGrade);
                        } else {
                            cancelEdit();
                            showError("Invalid Grade", "Please enter a valid grade between 1.00 and 5.00");
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

        // If a subject code was set before initialization, load it now
        if (selectedSubjectCode != null && selectedSubjectDesc != null) {
            subjCodeCombBox.setText(selectedSubjectCode);
            subDesclbl.setText(selectedSubjectDesc);
            loadStudentsBySubjectCode(selectedSubjectCode);
        }
    }

    // Add method to format grade input
    private String formatGradeInput(String input) {
        try {
            float gradeValue = Float.parseFloat(input);
            return gradeFormat.format(gradeValue);
        } catch (NumberFormatException e) {
            return input;
        }
    }

    private void setupRowHoverEffect() {
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
        try {
            float gradeValue = Float.parseFloat(grade);
            return gradeValue >= 1.0 && gradeValue <= 5.0;
        } catch (NumberFormatException e) {
            showError("Invalid Grade",
                    "Please enter a valid grade:\n" +
                            "• Must be between 1.00 and 5.00\n"
            );
            return false;
        }
    }

    private void updateGradeInDatabase(Student student) {
        Integer subjectId = getSubjectIdByCode(student.getSubjectCodeForGrade());
        if (subjectId == null) {
            showError("Error", "Could not find subject ID for " + student.getSubjectCodeForGrade());
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
            showError("Invalid Grade", "Grade format is invalid.");
            loadStudentsBySubjectCode(selectedSubjectCode); // Reload to revert
            return;
        }

        Integer gradeStatusId;
        if (gradeValue != null) {
            gradeStatusId = getGradeStatusId(gradeValue);
            if (gradeStatusId == null) {
                showError("Error", "Could not determine grade status ID for grade " + gradeValue);
                loadStudentsBySubjectCode(selectedSubjectCode); // Reload to revert
                return;
            }
        } else {
            gradeStatusId = null;
        }

        // If gradeValue is null, it means the grade is being cleared.
        // In this case, grade_status_id should also likely be null or a specific 'Not Graded' status.
        // For simplicity, if gradeValue is null, we'll set grade_status_id to null.

        String sql = "UPDATE grades SET grade_value = ?, grade_status_id = ? " +
                     "WHERE student_id = ? AND subject_id = ?";

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
                    
                    pstmt.setInt(3, student.getStudentId());
                    pstmt.setInt(4, subjectId);

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows == 0) {
                        // This might happen if the grade record didn't exist, which implies an issue
                        // or if the student is not enrolled in the subject (grades table might not have a row yet).
                        // For an edit grades page, a row should typically exist.
                        // Consider if an INSERT is needed if update fails (upsert logic), though less common for 'edit'.
                        System.err.println("No rows updated for student " + student.getStudentId() + ", subject " + subjectId + ". Grade record might not exist.");
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
                    e.printStackTrace();
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
             System.err.println("Task to update grade failed.");
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
            e.printStackTrace();
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
            System.err.println("Cannot determine grade status for value: " + gradeValue);
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
                System.err.println("Grade status ID not found in DB for status: " + statusName);
                Platform.runLater(() -> showError("DB Error", "Grade status '" + statusName + "' not found."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
            Platform.runLater(() -> showError("DB Error", "Failed to get grade status name for ID: " + gradeStatusId));
        }
        return null;
    }

    // Method to populate subject codes in the dropdown
    private void populateSubjectCodes() {
        subjCodeCombBox.getItems().clear(); // Clear existing items
        String sql = "SELECT subject_code, subject_description FROM subjects ORDER BY subject_code";

        Task<ObservableList<MenuItem>> task = new Task<>() {
            @Override
            protected ObservableList<MenuItem> call() throws Exception {
                ObservableList<MenuItem> subjectItems = FXCollections.observableArrayList();
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        String code = rs.getString("subject_code");
                        String description = rs.getString("subject_description");
                        MenuItem item = new MenuItem(code);
                        item.setUserData(description); // Store description for later use
                        item.setOnAction(event -> handleSubjectCodeSelection(code, description));
                        subjectItems.add(item);
                    }
                } catch (SQLException e) {
                    e.printStackTrace(); // Log error
                    Platform.runLater(() -> showError("Database Error", "Failed to load subject codes."));
                    throw e; // Re-throw to be caught by task's exception handling
                }
                return subjectItems;
            }
        };

        task.setOnSucceeded(event -> subjCodeCombBox.getItems().addAll(task.getValue()));
        task.setOnFailed(event -> {
            // Error already shown via Platform.runLater in the task
            System.err.println("Task to populate subject codes failed.");
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
        studentsList.clear();
        String sql = "SELECT s.student_id, s.student_no, s.first_name, s.middle_name, s.last_name, s.email, s.birthday, s.address, " +
                     "s.student_status_id, ss.status_name AS student_status_name, " +
                     "s.department_id, d.department_name AS program_name, " +
                     "s.year_level_id, yl.year_level_name, " +
                     "s.scholastic_status_id, scs.status_name AS scholastic_status_name, " +
                     "g.grade_value, subj.subject_code AS subject_code_for_grade, gs.status_name AS grade_status_name " +
                     "FROM students s " +
                     "JOIN grades g ON s.student_id = g.student_id " +
                     "JOIN subjects subj ON g.subject_id = subj.subject_id " +
                     "LEFT JOIN student_statuses ss ON s.student_status_id = ss.student_status_id " +
                     "LEFT JOIN departments d ON s.department_id = d.department_id " +
                     "LEFT JOIN year_levels yl ON s.year_level_id = yl.year_level_id " +
                     "LEFT JOIN scholastic_statuses scs ON s.scholastic_status_id = scs.scholastic_status_id " +
                     "LEFT JOIN grade_statuses gs ON g.grade_status_id = gs.grade_status_id " +
                     "WHERE subj.subject_code = ? ORDER BY s.last_name, s.first_name";

        Task<ObservableList<Student>> task = new Task<>() {
            @Override
            protected ObservableList<Student> call() throws Exception {
                ObservableList<Student> loadedStudents = FXCollections.observableArrayList();
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, subjectCode);
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        Integer studentId = rs.getInt("student_id");
                        String studentNo = rs.getString("student_no");
                        String firstName = rs.getString("first_name");
                        String middleName = rs.getString("middle_name");
                        String lastName = rs.getString("last_name");
                        String email = rs.getString("email");
                        Date dbBirthday = rs.getDate("birthday");
                        LocalDate birthday = (dbBirthday != null) ? dbBirthday.toLocalDate() : null;
                        String address = rs.getString("address");

                        Integer studentStatusId = rs.getObject("student_status_id") != null ? rs.getInt("student_status_id") : null;
                        String studentStatusName = rs.getString("student_status_name");

                        Integer departmentId = rs.getObject("department_id") != null ? rs.getInt("department_id") : null;
                        String departmentName = rs.getString("program_name");

                        Integer yearLevelId = rs.getObject("year_level_id") != null ? rs.getInt("year_level_id") : null;
                        String yearLevelName = rs.getString("year_level_name");

                        Integer scholasticStatusId = rs.getObject("scholastic_status_id") != null ? rs.getInt("scholastic_status_id") : null;
                        String scholasticStatusName = rs.getString("scholastic_status_name");

                        // Grade related fields
                        String finalGradeStr = null;
                        double gradeValue = rs.getDouble("grade_value");
                        if (!rs.wasNull()) {
                            finalGradeStr = gradeFormat.format(gradeValue);
                        } else {
                            // Handle case where grade_value might be NULL (e.g. not yet graded)
                            // You might want a specific string like "N/A" or empty
                            finalGradeStr = ""; // Or some placeholder
                        }
                        
                        String subjCodeForGrade = rs.getString("subject_code_for_grade");
                        String gradeStatusStr = rs.getString("grade_status_name");
                        if (gradeStatusStr == null && finalGradeStr != null && !finalGradeStr.isEmpty()) {
                            // If status is null but grade exists, try to determine (basic)
                            try {
                                double val = Double.parseDouble(finalGradeStr);
                                gradeStatusStr = (val >= 1.0 && val <= 3.0) ? "Passed" : "Failed";
                            } catch (NumberFormatException e) { /* ignore */ }
                        }

                        loadedStudents.add(new Student(
                                studentId, studentNo, firstName, middleName, lastName, email, birthday, address,
                                studentStatusId, studentStatusName, departmentId, departmentName,
                                yearLevelId, yearLevelName, scholasticStatusId, scholasticStatusName,
                                finalGradeStr, subjCodeForGrade, gradeStatusStr
                        ));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showError("Database Error", "Failed to load student data: " + e.getMessage()));
                    throw e;
                }
                return loadedStudents;
            }
        };

        task.setOnSucceeded(event -> {
            studentsList.setAll(task.getValue());
            studentsTable.setItems(studentsList);
            filterData(); // Apply existing search filter
        });
        task.setOnFailed(event -> {
            // Error already handled by Platform.runLater in task
            System.err.println("Task to load students failed.");
        });
        new Thread(task).start();
    }

    // Filter data in the table based on search bar input
    private void filterData() {
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