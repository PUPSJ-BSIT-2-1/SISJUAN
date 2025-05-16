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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EnrollmentController implements Initializable {

    @FXML
    private Button selectAllButton;

    @FXML
    private VBox subjectListContainer;

    private List<CheckBox> subjectCheckboxes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadSubjects();
    }
    
    @FXML
    private void handleSelectAll(ActionEvent event) {
        boolean allSelected = true;

        // Check if any checkbox is unchecked
        for (CheckBox checkbox : subjectCheckboxes) {
            if (!checkbox.isSelected()) {
                allSelected = false;
                break;
            }
        }

        // Toggle all checkboxes
        for (CheckBox checkbox : subjectCheckboxes) {
            checkbox.setSelected(!allSelected);
        }

        // Update button text based on state
        updateSelectAllButtonText(!allSelected);
    }

    private void updateSelectAllButtonState() {
        boolean allSelected = true;

        for (CheckBox checkbox : subjectCheckboxes) {
            if (!checkbox.isSelected()) {
                allSelected = false;
                break;
            }
        }

        updateSelectAllButtonText(allSelected);
    }

    private void updateSelectAllButtonText(boolean allSelected) {
        if (allSelected) {
            selectAllButton.setText("Deselect All");
        } else {
            selectAllButton.setText("Select All");
        }
    }

    private void loadSubjects() {
        // Clear existing contents
        subjectListContainer.getChildren().clear();
        subjectCheckboxes.clear();
        
        // Create a loading indicator
        Label loadingLabel = new Label("Loading subjects...");
        loadingLabel.getStyleClass().add("loading-text");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(30, 30);
        VBox loadingBox = new VBox(10, progressIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(20));
        subjectListContainer.getChildren().add(loadingBox);
        
        // Create a task for loading subjects in the background
        Task<List<SubjectData>> loadSubjectsTask = new Task<>() {
            @Override
            protected List<SubjectData> call() throws Exception {
                List<SubjectData> subjects = new ArrayList<>();
                try {
                    // Establish database connection
                    Connection connection = DBConnection.getConnection();
                    
                    // Query for all subjects with their professor names
                    String query = "SELECT s.subject_code, s.description, f.firstname, f.lastname " +
                                   "FROM subjects s " +
                                   "LEFT JOIN faculty f ON s.professor = f.faculty_id " +
                                   "ORDER BY s.subject_code";
                    
                    try (Statement stmt = connection.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {
                        
                        while (rs.next()) {
                            String subjectCode = rs.getString("subject_code");
                            String description = rs.getString("description");
                            String professorFirstName = rs.getString("firstname");
                            String professorLastName = rs.getString("lastname");
                            
                            String professorName = "";
                            if (professorFirstName != null && professorLastName != null) {
                                professorName = "Prof. " + professorFirstName + " " + professorLastName;
                            }
                            
                            subjects.add(new SubjectData(subjectCode, description, professorName));
                        }
                    }
                    
                    connection.close();
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw e; // Rethrow to be caught by onFailed
                }
                return subjects;
            }
        };
        
        // Handle task success
        loadSubjectsTask.setOnSucceeded(event -> {
            subjectListContainer.getChildren().clear(); // Remove the loading indicator
            List<SubjectData> subjects = loadSubjectsTask.getValue();
            
            if (subjects.isEmpty()) {
                showAlert("No Available Subjects", 
                         "There are no subjects available for enrollment at this time.");
            } else {
                // Add all subjects to the UI
                for (SubjectData subject : subjects) {
                    addSubjectRow(subject.code, subject.description, subject.professor);
                }
            }
        });
        
        // Handle task failure
        loadSubjectsTask.setOnFailed(event -> {
            subjectListContainer.getChildren().clear(); // Remove the loading indicator
            Throwable exception = loadSubjectsTask.getException();
            exception.printStackTrace();
            showAlert("Database Error", 
                    "Could not retrieve subjects from database. Please try again later or contact support.\n\nError: " + 
                    exception.getMessage());
        });
        
        // Start the background task
        Thread thread = new Thread(loadSubjectsTask);
        thread.setDaemon(true); // Set as daemon so it doesn't prevent application from exiting
        thread.start();
    }

    private void addSubjectRow(String subjectCode, String description, String professor) {
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
        
        // Subject code
        Label codeLabel = new Label(subjectCode);
        codeLabel.setPrefWidth(200);
        codeLabel.getStyleClass().add("subject-code");
        
        // Description with professor
        String fullDescription = description;
        if (professor != null && !professor.isEmpty()) {
            fullDescription += " - " + professor;
        }
        Label descLabel = new Label(fullDescription);
        descLabel.setPrefWidth(300);
        descLabel.getStyleClass().add("subject-description");
        descLabel.setWrapText(true);
        
        // Schedule options
        ComboBox<String> scheduleCombo = new ComboBox<>();
        scheduleCombo.getItems().addAll("8:00 AM - 10:00 AM", "10:00 AM - 12:00 PM", "1:00 PM - 3:00 PM", "3:00 PM - 5:00 PM");
        scheduleCombo.setPrefWidth(180);
        scheduleCombo.setPrefHeight(30);
        scheduleCombo.setPromptText("Select Schedule");
        scheduleCombo.getStyleClass().addAll("modern-combo", "no-gray-disabled");
        scheduleCombo.setDisable(true);
        
        // Enable/disable schedule combo box based on checkbox state
        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            scheduleCombo.setDisable(!newValue);
        });
        
        // Add the components to the row
        row.getChildren().addAll(checkbox, codeLabel, descLabel, scheduleCombo);
        
        // Add the row to the container
        subjectListContainer.getChildren().add(row);
        
        // Add separator
        Separator separator = new Separator();
        subjectListContainer.getChildren().add(separator);
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

    // Helper class to store subject data
    private static class SubjectData {
        String code;
        String description;
        String professor;
        
        public SubjectData(String code, String description, String professor) {
            this.code = code;
            this.description = description;
            this.professor = professor;
        }
    }
}
