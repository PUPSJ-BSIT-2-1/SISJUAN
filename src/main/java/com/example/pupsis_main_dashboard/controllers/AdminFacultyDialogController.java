package com.example.pupsis_main_dashboard.controllers;
import com.example.pupsis_main_dashboard.models.Department;
import com.example.pupsis_main_dashboard.models.FacultyStatus;

import com.example.pupsis_main_dashboard.models.Faculty;
import com.example.pupsis_main_dashboard.utilities.FacultyDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;

public class AdminFacultyDialogController {

    @FXML private TextField facultyIdField;
    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<Department> departmentComboBox;
    @FXML private TextField emailField;
    @FXML private TextField contactField;
    @FXML private DatePicker birthdatePicker;
    @FXML private ComboBox<FacultyStatus> statusComboBox;
    @FXML private DatePicker dateJoinedPicker;

    private Stage dialogStage;
    private Faculty faculty;
    private boolean saveClicked = false;
    private FacultyDAO facultyDAO;
    private String errorMessage = "";

    @FXML
    private void initialize() {
        try {
            departmentComboBox.setItems(FXCollections.observableArrayList(FacultyDAO.getAllDepartments()));
        } catch (SQLException e) {
            showErrorAlert("Error loading department list:\n" + e.getMessage());
        }
        try {
            statusComboBox.setItems(FXCollections.observableArrayList(FacultyDAO.getAllFacultyStatuses()));
        } catch (SQLException e) {
            showErrorAlert("Error loading status list:\n" + e.getMessage());
        }
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

            // Set selected department
            if (faculty.getDepartmentId() != null && departmentComboBox.getItems() != null) {
                for (Department dep : departmentComboBox.getItems()) {
                    if (dep.getDepartmentId() == faculty.getDepartmentId()) {
                        departmentComboBox.setValue(dep);
                        break;
                    }
                }
            } else {
                departmentComboBox.setValue(null);
            }
           // for debugging only System.out.println("Faculty's current Department ID: " + faculty.getDepartmentId());

            emailField.setText(faculty.getEmail());
            contactField.setText(faculty.getContactNumber());
            birthdatePicker.setValue(faculty.getBirthdate());

            // Set selected status
            if (faculty.getFacultyStatusId() != null && statusComboBox.getItems() != null) {
                for (FacultyStatus stat : statusComboBox.getItems()) {
                    if (stat.getFacultyStatusId() == faculty.getFacultyStatusId()) {
                        statusComboBox.setValue(stat);
                        break;
                    }
                }
            } else {
                statusComboBox.setValue(null);
            }

            dateJoinedPicker.setValue(faculty.getDateJoined());
            facultyIdField.setDisable(true);
        } else {
            facultyIdField.clear();
            firstNameField.clear();
            middleNameField.clear();
            lastNameField.clear();
            departmentComboBox.setValue(null);
            emailField.clear();
            contactField.clear();
            birthdatePicker.setValue(null);
            statusComboBox.setValue(null);
            dateJoinedPicker.setValue(LocalDate.now());
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
        if (!isInputValid()) return;

        Department selectedDepartment = departmentComboBox.getValue();
        FacultyStatus selectedStatus = statusComboBox.getValue();

        if (selectedDepartment == null || selectedStatus == null) {
            showErrorAlert("Department and Status must be selected.");
            return;
        }

        boolean isNew = (this.faculty == null);

        if (isNew) {
            String facultyNum = facultyIdField.getText();

            // Double-check for duplicate BEFORE trying to add
            if (facultyDAO.facultyNumberExists(facultyNum)) {
                showErrorAlert("A faculty with this Faculty Number already exists. Please use a unique Faculty Number.");
                return;
            }

            // Build new Faculty
            this.faculty = new Faculty(
                    facultyNum, null,
                    firstNameField.getText(),
                    middleNameField.getText(),
                    lastNameField.getText(),
                    selectedDepartment.getDepartmentId(),
                    selectedDepartment.getDepartmentName(),
                    emailField.getText(),
                    contactField.getText(),
                    birthdatePicker.getValue(),
                    selectedStatus.getFacultyStatusId(),
                    selectedStatus.getStatusName(),
                    dateJoinedPicker.getValue()
            );

            int result = facultyDAO.addFaculty(this.faculty);

            if (result == 1) {
                // Success: show only the success, then close
                saveClicked = true;
                showAlert("Success", "Faculty member added successfully.", Alert.AlertType.INFORMATION);
                dialogStage.close();
            } else if (result == -1) {
                // Should *never* hit this if pre-check worked, but for safety:
                showErrorAlert("A faculty with this Faculty Number already exists. Please use a unique Faculty Number.");
            } else {
                showErrorAlert("Failed to add faculty. Please check your input or try again.");
            }
        } else {
            // Edit/update path
            this.faculty.setFirstName(firstNameField.getText());
            this.faculty.setMiddleName(middleNameField.getText());
            this.faculty.setLastName(lastNameField.getText());
            this.faculty.setDepartmentId(selectedDepartment.getDepartmentId());
            this.faculty.setDepartmentName(selectedDepartment.getDepartmentName());
            this.faculty.setEmail(emailField.getText());
            this.faculty.setContactNumber(contactField.getText());
            this.faculty.setBirthdate(birthdatePicker.getValue());
            this.faculty.setFacultyStatusId(selectedStatus.getFacultyStatusId());
            this.faculty.setFacultyStatusName(selectedStatus.getStatusName());
            this.faculty.setDateJoined(dateJoinedPicker.getValue());
            boolean success = facultyDAO.updateFaculty(this.faculty);

            if (success) {
                saveClicked = true;
                showAlert("Success", "Faculty member updated successfully.", Alert.AlertType.INFORMATION);
                dialogStage.close();
            } else {
                showErrorAlert("Failed to update faculty data. Please check your input or try again.");
            }
        }
    }

    // Utility: simple success/info alert
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
        if (departmentComboBox.getValue() == null) {
            errorContent.append("Department must be selected.\n");
        }
        if (emailField.getText() == null || !emailField.getText().contains("@")) {
            errorContent.append("Valid Email is required.\n");
        }
        if (birthdatePicker.getValue() == null || birthdatePicker.getValue().isAfter(LocalDate.now())) {
            errorContent.append("Valid Birthdate is required.\n");
        }
        if (statusComboBox.getValue() == null) {
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
