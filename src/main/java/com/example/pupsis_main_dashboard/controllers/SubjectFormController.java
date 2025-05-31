package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.SubjectManagement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SubjectFormController {

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
        cancelButton.setOnAction(e -> ((Stage) cancelButton.getScene().getWindow()).close());

        saveButton.setOnAction(e -> {
            if (subject == null) {
                subject = new SubjectManagement(
                        subjectCodeField.getText(),
                        prerequisiteField.getText(),
                        equivCodeField.getText(),
                        descriptionField.getText(),
                        Double.parseDouble(unitField.getText()),
                        yearLevelCombo.getValue(),
                        semesterCombo.getValue()
                );
            } else {
                subject.setSubjectCode(subjectCodeField.getText());
                subject.setPrerequisite(prerequisiteField.getText());
                subject.setEquivSubjectCode(equivCodeField.getText());
                subject.setDescription(descriptionField.getText());
                subject.setUnit(Double.parseDouble(unitField.getText()));
                subject.setYearLevel(yearLevelCombo.getValue());
                subject.setSemester(semesterCombo.getValue());
            }

            ((Stage) saveButton.getScene().getWindow()).close();
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
}
