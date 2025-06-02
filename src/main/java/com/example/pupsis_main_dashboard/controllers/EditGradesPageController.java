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
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.text.DecimalFormat;

public class EditGradesPageController implements Initializable {

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
        // Add this check at the beginning of initializing
        if (gradesHeaderLbl == null) {
            System.err.println("Error: gradesHeaderLbl is null. Check FXML file for proper fx:id.");
        }
        
        if (studentsTable == null) {
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
                // Set the initial value with proper formatting
                String initialValue = getItem();
                try {
                    float gradeValue = Float.parseFloat(initialValue);
                    initialValue = gradeFormat.format(gradeValue);
                } catch (NumberFormatException e) {
                    // keep the original value if parsing fails
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

        // Add this line in the initialize() method after populateSubjectCodes();
        populateYearSections();

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
                float gradeValue = Float.parseFloat(newGrade);

                // Calculate grade status
                String gradeStatus;
                if (gradeValue >= 1.00 && gradeValue <= 3.00) {
                    gradeStatus = "P";
                } else if (gradeValue > 3.00 && gradeValue <= 5.00) {
                    gradeStatus = "F";
                } else {
                    gradeStatus = "INC";
                }

                // Set parameters for update
                pstmt.setFloat(1, gradeValue);
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

                    // Refresh the TableView
                    studentsTable.refresh();

                    // Show a success message with a formatted grade
                    showSuccess("Success", String.format(
                            "Grade successfully updated to: %s\nStatus: %s",
                            gradeFormat.format(gradeValue), gradeStatus));
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to update grade: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            showError("Invalid Grade Format", "Please enter a valid numeric grade");
            e.printStackTrace();
        }
    }

    private void loadStudentsBySubjectCode(String subjectCode) {
    Task<ObservableList<Student>> loadTask = new Task<>() {
        @Override
        protected ObservableList<Student> call() throws Exception {
            ObservableList<Student> tempList = FXCollections.observableArrayList();

            try (Connection conn = DBConnection.getConnection()) {
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
                    pstmt.setString(2, SessionData.getInstance().getStudentId());
                    
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

    loadTask.setOnFailed(e -> {
        Throwable ex = loadTask.getException();
        showError("Database Error", "Failed to load data: " + ex.getMessage());
        ex.printStackTrace();
    });

    new Thread(loadTask).start();
}

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
            pstmt.setString(1, SessionData.getInstance().getStudentId());
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
                    loadStudentsBySubjectCode(subjCode);
                });
                subjCodeCombBox.getItems().add(item);
            }
        }
    } catch (SQLException e) {
        System.err.println("Error loading subject codes: " + e.getMessage());
        e.printStackTrace();
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
                return subject.getSubjCode().toLowerCase().contains(lowerCaseFilter);// Does not match
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
        studentCache.remove(subjectCode);
    }

    @FXML
    private void handleRefresh() {
        if (selectedSubjectCode != null) {
            studentCache.remove(selectedSubjectCode);
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
                pstmt.setString(1, SessionData.getInstance().getStudentId());
                ResultSet rs = pstmt.executeQuery();

                yrSecCombBox.getItems().clear();

                // Add "All" option
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
            }
        } catch (SQLException e) {
            System.err.println("Error loading year sections: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load year sections: " + e.getMessage());
        }
    }

    // Helper method to format grades from a database
    private String formatGradeFromDB(String gradeStr) {
        if (gradeStr != null && !gradeStr.isEmpty()) {
            try {
                float gradeValue = Float.parseFloat(gradeStr);
                return gradeFormat.format(gradeValue);
            } catch (NumberFormatException e) {
                return gradeStr;
            }
        }
        return gradeStr;
    }
}