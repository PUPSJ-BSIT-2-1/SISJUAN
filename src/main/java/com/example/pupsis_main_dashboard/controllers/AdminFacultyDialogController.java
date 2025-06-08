package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Faculty;
import com.example.pupsis_main_dashboard.utilities.FacultyDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class AdminFacultyDialogController {

    @FXML private TextField facultyIdField;
    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> departmentComboBox;
    @FXML private TextField emailField;
    @FXML private TextField contactField;
    @FXML private DatePicker birthdatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private DatePicker dateJoinedPicker;

    private Stage dialogStage;
    private Faculty faculty;
    private boolean saveClicked = false;
    private FacultyDAO facultyDAO;
    private String errorMessage = "";

    @FXML
    private void initialize() {
        departmentComboBox.setItems(FXCollections.observableArrayList("Department A", "Department B"));
        statusComboBox.setItems(FXCollections.observableArrayList("Full-time", "Part-time"));
        dateJoinedPicker.setValue(LocalDate.now());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setFacultyDAO(FacultyDAO facultyDAO) {
        this.facultyDAO = facultyDAO;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;

        if (faculty != null) {
            facultyIdField.setText(faculty.getFacultyId());
            firstNameField.setText(faculty.getFirstName());
            middleNameField.setText(faculty.getMiddleName());
            lastNameField.setText(faculty.getLastName());
            departmentComboBox.setValue(faculty.getDepartmentName());
            emailField.setText(faculty.getEmail());
            contactField.setText(faculty.getContactNumber());
            birthdatePicker.setValue(faculty.getBirthdate());
            statusComboBox.setValue(faculty.getFacultyStatusName());
            dateJoinedPicker.setValue(faculty.getDateJoined());

            facultyIdField.setDisable(true);
        } else {
            this.faculty = new Faculty();
            facultyIdField.setDisable(false);
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            faculty.setFacultyId(facultyIdField.getText());
            faculty.setFirstName(firstNameField.getText());
            faculty.setMiddleName(middleNameField.getText());
            faculty.setLastName(lastNameField.getText());
            faculty.setEmail(emailField.getText());
            faculty.setContactNumber(contactField.getText());
            faculty.setBirthdate(birthdatePicker.getValue());
            faculty.setDateJoined(dateJoinedPicker.getValue());

            try {
                // Get IDs for selected department and status
                String selectedDepartmentName = departmentComboBox.getValue();
                // TODO: Uncomment and ensure FacultyDAO.getDepartmentIdByName is implemented
                // Integer departmentId = facultyDAO.getDepartmentIdByName(selectedDepartmentName); 
                // if (departmentId == null) {
                //     errorMessage = "Selected department is invalid or not found.";
                //     showErrorAlert(errorMessage);
                //     return;
                // }
                // faculty.setDepartmentId(departmentId);

                String selectedStatusName = statusComboBox.getValue();
                // TODO: Uncomment and ensure FacultyDAO.getFacultyStatusIdByName is implemented
                // Integer statusId = facultyDAO.getFacultyStatusIdByName(selectedStatusName); 
                // if (statusId == null) {
                //     errorMessage = "Selected status is invalid or not found.";
                //     showErrorAlert(errorMessage);
                //     return;
                // }
                // faculty.setFacultyStatusId(statusId);

                // Removed: faculty.setDepartment(departmentField.getText());
                // Removed: faculty.setStatus(statusComboBox.getValue());

                saveClicked = true;
                dialogStage.close();
            } catch (/*SQLException |*/ NullPointerException e) { // SQLException removed as DB calls are commented
                // errorMessage = "Database error when fetching Department/Status ID: " + e.getMessage();
                errorMessage = "FacultyDAO not initialized or dependent DAO methods not yet implemented. Cannot save Department/Status ID.";
                if (e instanceof NullPointerException && facultyDAO == null) {
                    errorMessage = "FacultyDAO not initialized. Cannot fetch Department/Status ID.";
                }
                showErrorAlert(errorMessage + "\n" + e.getMessage());
                e.printStackTrace(); // For debugging
            }
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
        StringBuilder errorContent = new StringBuilder();

        if (facultyIdField.getText() == null || facultyIdField.getText().isEmpty()) {
            errorContent.append("Faculty ID is required.\n");
        }
        if (firstNameField.getText() == null || firstNameField.getText().isEmpty()) {
            errorContent.append("First Name is required.\n");
        }
        if (lastNameField.getText() == null || lastNameField.getText().isEmpty()) {
            errorContent.append("Last Name is required.\n");
        }
        if (departmentComboBox.getValue() == null || departmentComboBox.getValue().isEmpty()) {
            errorContent.append("Department must be selected.\n");
        }
        if (emailField.getText() == null || !emailField.getText().contains("@")) {
            errorContent.append("Valid Email is required.\n");
        }
        if (birthdatePicker.getValue() == null || birthdatePicker.getValue().isAfter(LocalDate.now())) {
            errorContent.append("Valid Birthdate is required.\n");
        }
        if (statusComboBox.getValue() == null || statusComboBox.getValue().isEmpty()) {
            errorContent.append("Status must be selected.\n");
        }
        if (dateJoinedPicker.getValue() == null) {
            errorContent.append("Date Joined is required.\n");
        }

        if (!errorContent.isEmpty()) {
            errorMessage = errorContent.toString();
            showErrorAlert(errorMessage);
            return false;
        }
        errorMessage = "";
        return true;
    }

    private void showErrorAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(dialogStage);
        alert.setTitle("Invalid Fields or Error");
        alert.setHeaderText("Please correct the following or check system status:");
        alert.setContentText(content);
        alert.showAndWait();
    }
}
