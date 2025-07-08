package com.sisjuan.controllers;

import com.sisjuan.models.SubjectManagement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminSubjectDialogController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSubjectDialogController.class);

    @FXML private TextField subjectCodeField;
    @FXML private TextField prerequisiteField;
    @FXML private TextField descriptionField;
    @FXML private TextField unitField;
    @FXML private ComboBox<String> yearLevelCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private SubjectManagement subject;
    private boolean isEdit = false;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        logger.info("Initializing AdminSubjectDialogController");
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

        cancelButton.setOnAction(e -> {
            logger.info("Cancel button clicked. Closing form.");
            closeWindow();
        });

        saveButton.setOnAction(e -> handleSave());
    }

    public void showForm(SubjectManagement subject, Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
        if (subject != null) {
            logger.info("Showing form to edit subject: {}", subject.getSubjectCode());
            setSubject(subject);
        } else {
            logger.info("Showing form to add a new subject.");
            this.subject = null;
            this.isEdit = false;
        }
    }

    private void handleSave() {
        logger.info("Save button clicked.");
        try {
            double unitValue = Double.parseDouble(unitField.getText());

            if (!isEdit) {
                logger.debug("Creating new subject.");
                // For new subjects, ID can be a placeholder since the DB will generate it.
                this.subject = new SubjectManagement(
                        0,
                        subjectCodeField.getText(),
                        prerequisiteField.getText(),
                        descriptionField.getText(),
                        unitValue,
                        yearLevelCombo.getValue(),
                        semesterCombo.getValue()
                );
            } else {
                logger.debug("Updating existing subject: {}", subject.getSubjectCode());
                subject.setSubjectCode(subjectCodeField.getText());
                subject.setPrerequisite(prerequisiteField.getText());
                subject.setDescription(descriptionField.getText());
                subject.setUnit(unitValue);
                subject.setYearLevel(yearLevelCombo.getValue());
                subject.setSemester(semesterCombo.getValue());
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            closeWindow();

        } catch (NumberFormatException ex) {
            logger.error("Invalid input for Units field.", ex);
            showAlert("Invalid Input", "Please enter a valid number for Units.");
        }
    }

    private void saveToDatabase(boolean isNew) {
        // This method should contain the logic to persist the subject to the database
        // For now, we'll just log it and trigger the callback.
        logger.info("Saving subject to database (isNew={}).", isNew);
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
        closeWindow();
    }

    public void setSubject(SubjectManagement subject) {
        logger.info("Setting subject data in form for editing: {}", subject.getSubjectCode());
        this.subject = subject;
        this.isEdit = true;
        subjectCodeField.setText(subject.getSubjectCode());
        prerequisiteField.setText(subject.getPrerequisite());
        descriptionField.setText(subject.getDescription());
        unitField.setText(String.valueOf(subject.getUnit()));
        yearLevelCombo.setValue(subject.getYearLevel());
        semesterCombo.setValue(subject.getSemester());
    }

    public SubjectManagement getSubject() {
        logger.debug("Getting subject data from form.");
        return subject;
    }

    private void closeWindow() {
        logger.debug("Closing form window.");
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        logger.warn("Showing alert: Title='{}', Content='{}'", title, content);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}