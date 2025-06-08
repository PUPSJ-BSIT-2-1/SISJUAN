package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.SubjectManagement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AdminSubjectFormController {

    @FXML private TextField subjectCodeField;
    @FXML private TextField prerequisiteField;
    @FXML private TextField equivCodeField;
    @FXML private TextField descriptionField;
    @FXML private TextField unitField;
    @FXML private ComboBox<String> yearLevelCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private SubjectManagement subject;
    private boolean isEdit = false;

    @FXML
    public void initialize() {
        // Initialize ComboBox values to match filtering options
        yearLevelCombo.getItems().addAll(
            "1st Year",
            "2nd Year",
            "3rd Year",
            "4th Year"
        );
        
        semesterCombo.getItems().addAll(
            "1st Semester",
            "2nd Semester",
            "Summer Semester"
        );
        
        // Set default values if empty
        if (yearLevelCombo.getValue() == null) {
            yearLevelCombo.setValue("1st Year");
        }
        
        if (semesterCombo.getValue() == null) {
            semesterCombo.setValue("1st Semester");
        }

        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());

        saveButton.setOnAction(e -> {
            try {
                double unitValue = Double.parseDouble(unitField.getText());
                
                if (subject == null) {
                    subject = new SubjectManagement(
                            subjectCodeField.getText(),
                            prerequisiteField.getText(),
                            equivCodeField.getText(),
                            descriptionField.getText(),
                            unitValue,
                            yearLevelCombo.getValue(),
                            semesterCombo.getValue()
                    );
                } else {
                    subject.setSubjectCode(subjectCodeField.getText());
                    subject.setPrerequisite(prerequisiteField.getText());
                    subject.setEquivSubjectCode(equivCodeField.getText());
                    subject.setDescription(descriptionField.getText());
                    subject.setUnit(unitValue);
                    subject.setYearLevel(yearLevelCombo.getValue());
                    subject.setSemester(semesterCombo.getValue());
                }

                ((Stage) saveButton.getScene().getWindow()).close();
            } catch (NumberFormatException ex) {
                showAlert("Invalid Input", "Please enter a valid number for Units.");
            }
        });
    }

    public void setSubject(SubjectManagement subject) {
        this.subject = subject;
        this.isEdit = true;
        subjectCodeField.setText(subject.getSubjectCode());
        prerequisiteField.setText(subject.getPrerequisite());
        equivCodeField.setText(subject.getEquivSubjectCode());
        descriptionField.setText(subject.getDescription());
        unitField.setText(String.valueOf(subject.getUnit()));
        yearLevelCombo.setValue(subject.getYearLevel());
        semesterCombo.setValue(subject.getSemester());
    }

    public SubjectManagement getSubject() {
        return subject;
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}