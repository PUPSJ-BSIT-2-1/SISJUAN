package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.Student;
import com.example.pupsis_main_dashboard.utility.Subject;
import javafx.application.Application;

import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.converter.DefaultStringConverter;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import com.example.pupsis_main_dashboard.databaseOperations.dbConnection2;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import javafx.scene.input.KeyCode;

public class EditPageController implements Initializable {
    @FXML
    private MenuButton subjCodeCombBox;

    @FXML
    private MenuButton yrSecCombBox;

    @FXML
    private TableView<Student> studentsTable;

    @FXML
    private TableColumn<Student, String> noStudCol;

    @FXML
    private TableColumn<Student, String> studIDCol;

    @FXML
    private TableColumn<Student, String> studNameCol;

    @FXML
    private TableColumn<Student, String> subjCodeCol;

    @FXML
    private TableColumn<Student, String> finGradeCol;

    @FXML
    private TableColumn<Student, String> gradeStatCol;

    private ObservableList<Student> studentsList = FXCollections.observableArrayList();

    dbConnection2 dbConn = new dbConnection2();
    String url = dbConn.URL;
    String user = dbConn.USER;
    String password = dbConn.PASSWORD;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Make table editable
        studentsTable.setEditable(true);

        // Set up the columns
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

        // Load your data
        loadStudentData();
    }

    private boolean isValidGrade(String grade) {
        try {
            float gradeValue = Float.parseFloat(grade);
            return gradeValue >= 1.0 && gradeValue <= 5.0; // Adjust range as needed
        } catch (NumberFormatException e) {
            showError("Invalid Grade",
            "Please enter a valid grade:\n" +
            "• Must be between 1.00 and 5.00\n"
        );
            return false;
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
                                getClass().getResource("/com/example/pupsis_main_dashboard/fxml/newEditingGradePage.fxml")
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

private void updateGradeInDatabase(Student student) {
    try (Connection conn = DriverManager.getConnection(url, user, password)) {
        // Update both final grade and grade status
        String query = "UPDATE students_subj SET \"finalGrade\" = ?, \"gradeStat\" = ? " +
                      "WHERE \"student_ID\" = ? AND \"subj_Code\" = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            String newGrade = student.getFinalGrade();
            float gradeValue = Float.parseFloat(newGrade);
            
            // Calculate grade status
            String gradeStatus;
            if (gradeValue >= 1.00 && gradeValue <= 3.00) {
                gradeStatus = "PASSED";
            } else if (gradeValue > 3.00 && gradeValue <= 5.00) {
                gradeStatus = "FAILED";
            } else {
                gradeStatus = "INVALID";
            }
            
            // Set parameters for update - note the use of setFloat for finalGrade
            pstmt.setFloat(1, gradeValue);  // Changed from setString to setFloat
            pstmt.setString(2, gradeStatus);
            pstmt.setString(3, student.getStudentId());
            pstmt.setString(4, student.getSubjCode());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                // Update the student object's grade status
                student.setGradeStatus(gradeStatus);
                
                // Refresh the specific row in the table
                Student updatedStudent = student;
                int rowIndex = studentsList.indexOf(student);
                if (rowIndex >= 0) {
                    studentsList.set(rowIndex, updatedStudent);
                }
                
                // Refresh the TableView
                studentsTable.refresh();
                
                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText(String.format("Grade successfully updated to: %.2f\nStatus: %s", 
                                   gradeValue, gradeStatus));
                alert.showAndWait();
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

// Add this helper method to show success messages
private void showSuccess(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
}
    private void loadStudentData() {
        if (studentsTable == null) {
            System.err.println("Error: studentsTable is null. Check FXML file for proper fx:id.");
            return;
        }

        // Initialize the columns with the correct property names
        noStudCol.setCellValueFactory(new PropertyValueFactory<>("studentNo"));
        studIDCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        studNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjCode"));
        finGradeCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        gradeStatCol.setCellValueFactory(new PropertyValueFactory<>("gradeStatus"));

        // Populate the subject codes dropdown
        populateSubjectCodes();
        
        // Set default text for the MenuButton
        subjCodeCombBox.setText("Select Subject Code");

        studentsTable.getColumns().forEach(column -> column.setReorderable(false));
    }

    private void loadStudentsBySubjectCode(String subjectCode) {
        studentsList.clear();
        studentsTable.getItems().clear();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String query = "SELECT * FROM students_subj WHERE \"subj_Code\" = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, subjectCode);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Student student = new Student(
                                rs.getString("No."),
                                rs.getString("student_ID"),
                                rs.getString("student_name"),
                                rs.getString("subj_Code"),
                                rs.getString("finalGrade"),
                                rs.getString("gradeStat")
                        );
                        studentsList.add(student);
                    }
                    studentsTable.setItems(studentsList);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            showError("Database Error", "Failed to load student data: " + e.getMessage());
        }
    }

    private void populateSubjectCodes() {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Note the quoted column name "subject_code"
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

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}