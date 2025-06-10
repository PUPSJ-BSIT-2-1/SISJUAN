package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;

public class AdminAssignSubjectDialogController {

    // Inner record to hold section data
    public record SectionItem(int id, String name) {
        @Override
        public String toString() {
            return name; // This will be used by ComboBox default display if no converter is set
        }
    }

    // Inner record to hold semester data
    public record SemesterItem(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    @FXML
    private ComboBox<String> subjectComboBox;

    @FXML
    private ComboBox<SectionItem> sectionComboBox;

    @FXML
    private TextField semesterTextField;

    @FXML
    private TextField schoolYearTextField;

    private Stage dialogStage;
    private boolean assigned = false;

    @FXML
    public void initialize() {
        // Configure the ComboBox to display the name property of SectionItem
        sectionComboBox.setConverter(new StringConverter<SectionItem>() {
            @Override
            public String toString(SectionItem sectionItem) {
                return sectionItem == null ? null : sectionItem.name();
            }

            @Override
            public SectionItem fromString(String string) {
                // Not needed for a non-editable ComboBox, but good practice to implement
                // This would require looking up the SectionItem by name if it were editable
                return sectionComboBox.getItems().stream()
                        .filter(item -> item.name().equals(string))
                        .findFirst().orElse(null);
            }
        });
    }

    // Called by AdminFacultyManagementController to provide subject list
    public void setSubjects(List<String> subjects) {
        subjectComboBox.setItems(FXCollections.observableArrayList(subjects));
    }

    // Called by AdminFacultyManagementController to provide sections
    public void setSections(List<SectionItem> sections) {
        sectionComboBox.setItems(FXCollections.observableArrayList(sections));
    }

    public void setSchoolYearAndSemester(String schoolYear, String semester) {
        schoolYearTextField.setText(schoolYear);
        semesterTextField.setText(semester);
    }

    // For AdminFacultyManagementController to get selected subject ID
    public String getSelectedSubjectCode() {
        return subjectComboBox.getValue();
    }

    // For AdminFacultyManagementController to get selected year level's ID
    public int getSelectedSectionId() {
        SectionItem selectedSection = sectionComboBox.getValue();
        return selectedSection != null ? selectedSection.id() : -1; // Return -1 or throw if null, based on desired error handling
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
        if (subjectComboBox.getValue() == null || sectionComboBox.getValue() == null) {
            // Simple validation: all fields required
            // You can replace with a nicer alert dialog if you want
            StageAndSceneUtils.showAlert("Error", "Please select a subject and section.", Alert.AlertType.ERROR);
            return;
        }
        assigned = true;
        dialogStage.close();
    }

}
