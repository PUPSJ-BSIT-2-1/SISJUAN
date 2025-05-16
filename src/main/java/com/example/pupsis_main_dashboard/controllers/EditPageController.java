package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.StudentCache;
import com.example.pupsis_main_dashboard.databaseOperations.dbConnection2;
import com.example.pupsis_main_dashboard.utility.Student;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.Objects;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

public class EditPageController implements Initializable {

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
    private final StudentCache studentCache = StudentCache.getInstance();
    private String selectedSubjectCode;
    private String selectedSubjectDesc;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (studentsTable == null) {
            System.err.println("Error: studentsTable is null. Check FXML file for proper fx:id.");
            return;
        }

        // Make table editable
        studentsTable.setEditable(true);

        // Initialize the columns with the correct property names
        noStudCol.setCellValueFactory(new PropertyValueFactory<>("studentNo"));
        studIDCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        studNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
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
                    setText(item);
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
                setText(getItem());
                setGraphic(null);
            }

            private void createTextField() {
                textField = new TextField(getItem());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

                textField.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ENTER) {
                        if (isValidGrade(textField.getText())) {
                            commitEdit(textField.getText());
                        } else {
                            cancelEdit();
                            showError("Invalid Grade", "Please enter a valid grade between 1.0 and 5.0");
                        }
                    } else if (e.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        if (isValidGrade(textField.getText())) {
                            commitEdit(textField.getText());
                        } else {
                            cancelEdit();
                            showError("Invalid Grade", "Please enter a valid grade between 1.0 and 5.0");
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

        // Set default text for the MenuButton
        subjCodeCombBox.setText("Select Subject Code");

        // If a subject code was set before initialization, load it now
        if (selectedSubjectCode != null) {
            subjCodeCombBox.setText(selectedSubjectCode);
            loadStudentsBySubjectCode(selectedSubjectCode);
        }
    }

    private void setupRowClickHandler() {
        studentsTable.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    try {
                        // Get the parent ScrollPane (contentPane)
                        ScrollPane contentPane = (ScrollPane) studentsTable.getScene().lookup("#contentPane");

                        if (contentPane != null) {
                            // Load the editing grade page
                            Parent newContent = FXMLLoader.load(Objects.requireNonNull(
                                    getClass().getResource("/com/example/GradingModule/fxml/newEditingGradePage.fxml")
                            ));
                            contentPane.setContent(newContent);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
        try (Connection conn = dbConnection2.getConnection()) {
            // Update both final grade and grade status
            String query = "UPDATE students_subj SET \"finalGrade\" = ?, \"gradeStat\" = ? " +
                    "WHERE \"student_ID\" = ? AND \"subj_Code\" = ?";

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

                    // Show success message
                    showSuccess("Success", String.format(
                            "Grade successfully updated to: %.2f\nStatus: %s",
                            gradeValue, gradeStatus));
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
        // Check cache first
        ObservableList<Student> cachedStudents = StudentCache.get(subjectCode);
        if (cachedStudents != null) {
            updateTableView(cachedStudents);
            return;
        }

        // Run database operation in background thread
        Task<ObservableList<Student>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Student> call() throws Exception {
                ObservableList<Student> tempList = FXCollections.observableArrayList();

                try (Connection conn = dbConnection2.getConnection()) {
                    String query = """
                    SELECT ss."No.", ss."student_ID", ss."student_name", 
                           ss."subj_Code", ss."finalGrade", ss."gradeStat"
                    FROM students_subj ss
                    WHERE ss."subj_Code" = ?
                    ORDER BY CAST(ss."No." AS INTEGER)""";

                    try (PreparedStatement pstmt = conn.prepareStatement(query,
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY)) {

                        pstmt.setFetchSize(100);
                        pstmt.setString(1, subjectCode);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                if (isCancelled()) break;

                                Student student = new Student(
                                        rs.getString("No."),
                                        rs.getString("student_ID"),
                                        rs.getString("student_name"),
                                        rs.getString("subj_Code"),
                                        rs.getString("finalGrade"),
                                        rs.getString("gradeStat")
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
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            showError("Database Error", "Failed to load data: " + ex.getMessage());
            ex.printStackTrace();
        });

        new Thread(loadTask).start();
    }

    private void populateSubjectCodes() {
        try (Connection conn = dbConnection2.getConnection()) {
            String query = "SELECT DISTINCT \"subject_code\" FROM subjects2";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String subjCode = rs.getString("subject_code");
                    javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(subjCode);
                    item.setOnAction(event -> {
                        subjCodeCombBox.setText(subjCode);
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
}