package com.example.pupsis_main_dashboard.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

    @FXML
    private ComboBox<String> subjectComboBox;

    @FXML
    private ComboBox<SectionItem> yearLevelComboBox;

    @FXML
    private Button cancelButton;

    @FXML
    private Button assignButton;

    private Stage dialogStage;
    private boolean assigned = false;

    @FXML
    public void initialize() {
        // Configure the ComboBox to display the name property of SectionItem
        yearLevelComboBox.setConverter(new StringConverter<SectionItem>() {
            @Override
            public String toString(SectionItem sectionItem) {
                return sectionItem == null ? null : sectionItem.name();
            }

            @Override
            public SectionItem fromString(String string) {
                // Not needed for a non-editable ComboBox, but good practice to implement
                // This would require looking up the SectionItem by name if it were editable
                return yearLevelComboBox.getItems().stream()
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
        yearLevelComboBox.setItems(FXCollections.observableArrayList(sections));
    }

    // For AdminFacultyManagementController to get selected subject ID
    public String getSelectedSubjectCode() {
        return subjectComboBox.getValue();
    }

    // For AdminFacultyManagementController to get selected year level's ID
    public int getSelectedSectionId() {
        SectionItem selectedSection = yearLevelComboBox.getValue();
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
        if (subjectComboBox.getValue() == null || yearLevelComboBox.getValue() == null) {
            // Simple validation: all fields required
            // You can replace with a nicer alert dialog if you want
            System.out.println("Please select subject and section.");
            return;
        }
        assigned = true;
        dialogStage.close();
    }

    // Helper method for alerts (assuming you might want to add one)
    private void showAlert(String title, String content, javafx.scene.control.Alert.AlertType alertType) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
