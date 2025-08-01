package com.sisjuan.controllers;

import com.sisjuan.models.Faculty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AdminFacultyDetailsController {

    @FXML private Label idLabel;
    @FXML private Label firstNameLabel;
    @FXML private Label middleNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label departmentLabel;
    @FXML private Label emailLabel;
    @FXML private Label contactLabel;
    @FXML private Label birthdateLabel;
    @FXML private Label dateJoinedLabel;
    @FXML private Label statusLabel;

    private Stage dialogStage;

    // Called from AdminFacultyManagementController to pass the Stage reference
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    // Called from AdminFacultyManagementController to pass the Faculty data to display
    public void setFaculty(Faculty faculty) {

        for (Label label : new Label[]{idLabel, firstNameLabel, middleNameLabel, lastNameLabel, departmentLabel, emailLabel, contactLabel, birthdateLabel, dateJoinedLabel, statusLabel}) {
            label.getStyleClass().add("view-text");
        }
        idLabel.setText(faculty.getFacultyId());
        firstNameLabel.setText(faculty.getFirstName());
        middleNameLabel.setText(faculty.getMiddleName());
        lastNameLabel.setText(faculty.getLastName());
        departmentLabel.setText(faculty.getDepartmentName());
        emailLabel.setText(faculty.getEmail());
        contactLabel.setText(faculty.getContactNumber());
        birthdateLabel.setText(faculty.getBirthdate() != null ? faculty.getBirthdate().toString() : "");
        dateJoinedLabel.setText(faculty.getDateJoined() != null ? faculty.getDateJoined().toString() : "");
        statusLabel.setText(faculty.getFacultyStatusName());
    }

    // Close the button handler, closes the modal window
    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
