package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Faculty;
import com.example.pupsis_main_dashboard.models.FacultyAssignment;
import com.example.pupsis_main_dashboard.utilities.FacultyLoadDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AdminFacultyAssignmentsDialogController {

    @FXML private Label facultyLabel;
    @FXML private TableView<FacultyAssignment> assignmentsTable;
    @FXML private TableColumn<FacultyAssignment, String> subjectCodeColumn;
    @FXML private TableColumn<FacultyAssignment, String> subjectDescColumn;
    @FXML private TableColumn<FacultyAssignment, String> sectionColumn;
    @FXML private TableColumn<FacultyAssignment, String> semesterColumn;
    @FXML private TableColumn<FacultyAssignment, String> yearLevelColumn;

    private Faculty faculty;
    private FacultyLoadDAO facultyLoadDAO; // for DB calls
    private Stage dialogStage;

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
        if (faculty != null) {
            facultyLabel.setText("Assigned Subjects for " + faculty.getFirstName() + " " + faculty.getLastName());
            loadAssignments();
        }
    }

    public void setFacultyLoadDAO(FacultyLoadDAO facultyLoadDAO) {
        this.facultyLoadDAO = facultyLoadDAO;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void initialize() {
        // Bind columns to FacultyAssignment properties
        subjectCodeColumn.setCellValueFactory(data -> data.getValue().subjectCodeProperty());
        subjectDescColumn.setCellValueFactory(data -> data.getValue().subjectDescProperty());
        sectionColumn.setCellValueFactory(data -> data.getValue().sectionProperty());
        semesterColumn.setCellValueFactory(data -> data.getValue().semesterProperty());
        yearLevelColumn.setCellValueFactory(data -> data.getValue().yearLevelProperty());
    }

    // In AdminFacultyAssignmentsDialogController.java
    private void loadAssignments() {
        if (facultyLoadDAO == null || faculty == null) return;
        List<FacultyAssignment> assignments = facultyLoadDAO.getAssignmentsForFaculty(faculty.getActualFacultyId());
        assignmentsTable.setItems(FXCollections.observableArrayList(assignments));
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) dialogStage.close();
        else assignmentsTable.getScene().getWindow().hide();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
