package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Faculty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class FacultyDialogController {

    @FXML private TextField facultyIdField;
    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField departmentField;
    @FXML private TextField emailField;
    @FXML private TextField contactField;
    @FXML private DatePicker birthdatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker dateJoinedPicker;

    private Stage dialogStage;
    private Faculty faculty;
    private boolean okClicked = false;

    @FXML
    private void initialize() {
        statusComboBox.getItems().addAll("Full-time", "Part-time");
        dateJoinedPicker.setValue(LocalDate.now());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;

        if (faculty != null) {
            facultyIdField.setText(faculty.getFacultyId());
            firstNameField.setText(faculty.getFirstName());
            middleNameField.setText(faculty.getMiddleName());
            lastNameField.setText(faculty.getLastName());
            departmentField.setText(faculty.getDepartment());
            emailField.setText(faculty.getEmail());
            contactField.setText(faculty.getContactNumber());
            birthdatePicker.setValue(faculty.getBirthdate());
            statusComboBox.setValue(faculty.getStatus());
            dateJoinedPicker.setValue(faculty.getDateJoined());

            facultyIdField.setDisable(true); // Disable editing of ID during edit
        } else {
            facultyIdField.setDisable(false); // Allow editing when adding
        }
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            if (faculty == null) {
                faculty = new Faculty();
            }

            faculty.setFacultyId(facultyIdField.getText());
            faculty.setFirstName(firstNameField.getText());
            faculty.setMiddleName(middleNameField.getText());
            faculty.setLastName(lastNameField.getText());
            faculty.setDepartment(departmentField.getText());
            faculty.setEmail(emailField.getText());
            faculty.setContactNumber(contactField.getText());
            faculty.setBirthdate(birthdatePicker.getValue());
            faculty.setStatus(statusComboBox.getValue());
            faculty.setDateJoined(dateJoinedPicker.getValue());

            okClicked = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    public Faculty getFaculty() {
        return faculty;
    }

    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();

        if (facultyIdField.getText() == null || facultyIdField.getText().isEmpty())
            errorMessage.append("Faculty ID is required.\n");

        if (firstNameField.getText() == null || firstNameField.getText().isEmpty())
            errorMessage.append("First Name is required.\n");

        if (lastNameField.getText() == null || lastNameField.getText().isEmpty())
            errorMessage.append("Last Name is required.\n");

        if (emailField.getText() == null || !emailField.getText().contains("@"))
            errorMessage.append("Valid Email is required.\n");

        if (birthdatePicker.getValue() == null || birthdatePicker.getValue().isAfter(LocalDate.now()))
            errorMessage.append("Valid Birthdate is required.\n");

        if (statusComboBox.getValue() == null || statusComboBox.getValue().isEmpty())
            errorMessage.append("Status must be selected.\n");

        if (!errorMessage.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct the following:");
            alert.setContentText(errorMessage.toString());
            alert.showAndWait();
            return false;
        }

        return true;
    }
}
