package com.example.pupsis_main_dashboard.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.util.List;

public class AdminAssignSubjectDialogController {

    @FXML
    private ComboBox<String> subjectComboBox;

    @FXML
    private ComboBox<String> yearLevelComboBox;

    @FXML
    private ComboBox<String> semesterComboBox;

    @FXML
    private Button cancelButton;

    @FXML
    private Button assignButton;

    private Stage dialogStage;
    private boolean assigned = false;

    // Called by AdminFacultyManagementController to provide subject list
    public void setSubjects(List<String> subjects) {
        subjectComboBox.setItems(FXCollections.observableArrayList(subjects));
    }

    // Called by AdminFacultyManagementController to provide year levels (hardcoded or fetched)
    public void setYearLevels(List<String> yearLevels) {
        yearLevelComboBox.setItems(FXCollections.observableArrayList(yearLevels));
    }

    // Called by AdminFacultyManagementController to provide semesters (hardcoded or fetched)
    public void setSemesters(List<String> semesters) {
        semesterComboBox.setItems(FXCollections.observableArrayList(semesters));
    }

    // For AdminFacultyManagementController to get selected subject ID
    public String getSelectedSubjectId() {
        return subjectComboBox.getValue();
    }

    // For AdminFacultyManagementController to get selected year level
    public String getSelectedYearLevel() {
        return yearLevelComboBox.getValue();
    }

    // For AdminFacultyManagementController to get selected semester
    public String getSelectedSemester() {
        return semesterComboBox.getValue();
    }

    // Dialog stage setter to allow dialog control
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // Indicates if Assign was confirmed
    public boolean isAssigned() {
        return assigned;
    }

    // Cancel button action: close dialog without assigning
    @FXML
    private void handleCancel() {
        assigned = false;
        dialogStage.close();
    }

    // Assign button action: validate selections, then close with confirmation
    @FXML
    private void handleAssign() {
        if (subjectComboBox.getValue() == null || yearLevelComboBox.getValue() == null || semesterComboBox.getValue() == null) {
            // Simple validation: all fields required
            // You can replace with a nicer alert dialog if you want
            System.out.println("Please select subject, year level, and semester.");
            return;
        }
        assigned = true;
        dialogStage.close();
    }
}
