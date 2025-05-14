package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.Student;
import com.example.pupsis_main_dashboard.utility.Subject;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.converter.DefaultStringConverter;
import java.net.URL;
import java.util.ResourceBundle;
import com.example.pupsis_main_dashboard.databaseOperations.dbConnection2;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

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

    public void initialize(URL url, ResourceBundle rb) {
        // Make table editable
        studentsTable.setEditable(true);

        // Set up the final grade column to be editable
        finGradeCol.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        finGradeCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        finGradeCol.setOnEditCommit(event -> {
            try {
                Student student = event.getRowValue();
                String newValue = event.getNewValue();
                
                // Add validation for grade input
                if (isValidGrade(newValue)) {
                    student.setFinalGrade(newValue);
                    // Add your database update logic here
                    updateGradeInDatabase(student);
                } else {
                    // If invalid, refresh the table to show original value
                    studentsTable.refresh();
                }
            } catch (Exception e) {
                // Handle any errors
                studentsTable.refresh();
            }
        });

        // Make sure other columns remain non-editable
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
        // Check if grade is within valid range (1.0 to 5.0)
        if (gradeValue >= 1.0 && gradeValue <= 5.0) {
            // Check if grade has correct decimal places (e.g., 1.00, 1.25, 1.50, 1.75)
            String[] parts = grade.split("\\.");
            if (parts.length == 2) {
                int decimalPart = Integer.parseInt(parts[1]);
                return decimalPart == 0 || decimalPart == 25 || decimalPart == 50 || decimalPart == 75;
            }
            return false;
        }
        return false;
    } catch (NumberFormatException e) {
        return false;
    }
}

private void updateGradeInDatabase(Student student) {
    try (Connection conn = DriverManager.getConnection(url, user, password)) {
        // SQL query to update the final grade
        String query = "UPDATE students_subj SET \"finalGrade\" = ?, \"gradeStat\" = ? " +
                      "WHERE \"student_ID\" = ? AND \"subj_Code\" = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            float finalGrade = Float.parseFloat(student.getFinalGrade());
            
            // Set the parameters
            pstmt.setString(1, student.getFinalGrade());
            // Calculate grade status based on final grade
            String gradeStatus = calculateGradeStatus(finalGrade);
            pstmt.setString(2, gradeStatus);
            pstmt.setString(3, student.getStudentId());
            pstmt.setString(4, student.getSubjCode());
            
            // Execute the update
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Update was successful
                student.setGradeStatus(gradeStatus); // Update the UI
                studentsTable.refresh(); // Refresh the table view
                showSuccess("Grade Updated", "The grade has been successfully updated.");
            } else {
                showError("Update Failed", "No records were updated in the database.");
            }
        }
    } catch (SQLException e) {
        System.err.println("Error updating grade: " + e.getMessage());
        showError("Database Error", "Failed to update grade: " + e.getMessage());
    } catch (NumberFormatException e) {
        showError("Invalid Input", "Please enter a valid numeric grade.");
    }
}

// Helper method to calculate grade status
private String calculateGradeStatus(float grade) {
    if (grade >= 1.0 && grade <= 3.0) {
        return "PASSED";
    } else if (grade > 3.0 && grade <= 5.0) {
        return "FAILED";
    } else {
        return "INVALID";
    }
}

// Method to show success message
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