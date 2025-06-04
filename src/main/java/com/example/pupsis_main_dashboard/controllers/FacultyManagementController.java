package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Faculty;
import com.example.pupsis_main_dashboard.utilities.SubjectDAO;
import com.example.pupsis_main_dashboard.utilities.FacultyDAO;
import com.example.pupsis_main_dashboard.utilities.FacultyLoadDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class FacultyManagementController {

    @FXML private TableView<Faculty> facultyTable;
    @FXML private TableColumn<Faculty, String> idColumn;
    @FXML private TableColumn<Faculty, String> nameColumn;
    @FXML private TableColumn<Faculty, String> departmentColumn;
    @FXML private TableColumn<Faculty, String> emailColumn;
    @FXML private TableColumn<Faculty, String> contactColumn;
    @FXML private TableColumn<Faculty, Void> detailsColumn;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private Button assignSubjectButton;

    @FXML private Button exportCSVButton;
    @FXML private Button printReportButton;
    @FXML private Button backButton;
    @FXML private TextField searchField;

    private final ObservableList<Faculty> facultyList = FXCollections.observableArrayList();
    private FacultyDAO facultyDAO;
    private FacultyLoadDAO facultyLoadDAO;  // <-- Added declaration here
    private ScrollPane contentPane;

    public void initialize() {
        try {
            facultyDAO = new FacultyDAO();
            facultyLoadDAO = new FacultyLoadDAO();  // <-- Initialize here
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to the database.", Alert.AlertType.ERROR);
            return;
        }


        // Go back to the dashboard when the back button is clicked
        backButton.setOnAction(_ -> {
            handleBackToDashboard();
            resetScrollPosition();
        });

        idColumn.setCellValueFactory(new PropertyValueFactory<>("facultyId"));
        nameColumn.setCellValueFactory(cellData -> {
            Faculty faculty = cellData.getValue();
            String mi = (faculty.getMiddleName() != null && !faculty.getMiddleName().isEmpty())
                    ? faculty.getMiddleName().substring(0, 1).toUpperCase() + "." : "";
            String fullName = faculty.getFirstName() + " " + mi + " " + faculty.getLastName();
            return new SimpleStringProperty(fullName.trim());
        });
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());

        facultyTable.setItems(facultyList);
        loadFacultyData();
        detailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("➕");

            {
                btn.setOnAction(event -> {
                    Faculty faculty = getTableView().getItems().get(getIndex());
                    showFacultyDetailsModal(faculty);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private void showFacultyDetailsModal(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/FacultyDetailsTableView.fxml"));
            Parent root = loader.load();

            FacultyDetailsController controller = loader.getController();
            controller.setFaculty(faculty);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Faculty Details - " + faculty.getFirstName() + " " + faculty.getLastName());
            stage.setScene(new Scene(root));
            controller.setDialogStage(stage);  // so the close button can close this window
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open faculty details window.", Alert.AlertType.ERROR);
        }
    }


    private void loadFacultyData() {
        facultyList.clear();
        try {
            List<Faculty> list = facultyDAO.getAllFaculty();
            facultyList.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAssignSubject() {
        Faculty selectedFaculty = facultyTable.getSelectionModel().getSelectedItem();
        if (selectedFaculty == null) {
            showAlert("Warning", "Please select a faculty member first.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AssignSubjectDialog.fxml"));
            Parent root = loader.load();

            AssignSubjectDialogController controller = loader.getController();

            // Load subjects from your database or DAO
            List<String> subjects = loadSubjectCodes();
            System.out.println("Loaded subjects: " + subjects);  // DEBUG

            // Set subjects in dialog
            controller.setSubjects(subjects);

            // Use actual year_section values matching DB foreign key constraint
            List<String> yearLevels = List.of("1-1", "1-2", "2-1", "2-2", "3-1", "3-2");
            controller.setYearLevels(yearLevels);
            System.out.println("Set year levels: " + yearLevels);  // DEBUG

            // Hardcoded semesters, replace with actual if needed
            List<String> semesters = List.of("1st Semester", "2nd Semester");
            controller.setSemesters(semesters);
            System.out.println("Set semesters: " + semesters);  // DEBUG

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Assign Subject");
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isAssigned()) {
                String subjectCode = controller.getSelectedSubjectId();
                String yearSection = controller.getSelectedYearLevel();
                String semester = controller.getSelectedSemester();

                int facultyId = Integer.parseInt(selectedFaculty.getFacultyId());
                SubjectDAO subjectDAO = new SubjectDAO();
                int subjectId = subjectDAO.getSubjectIdByCode(subjectCode);
                String academicYear = "2023-2024";  // Or retrieve dynamically

                boolean success = facultyLoadDAO.addFacultyLoad(
                        facultyId,
                        subjectId,
                        yearSection,
                        semester,
                        academicYear
                );

                if (success) {
                    showAlert("Success", "Subject assigned successfully.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to assign subject.", Alert.AlertType.ERROR);
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open Assign Subject dialog or retrieve subject ID.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Loads subject codes from the database.
     */
    private List<String> loadSubjectCodes() {
        try {
            SubjectDAO subjectDAO = new SubjectDAO();
            List<String> subjectCodes = subjectDAO.getAllSubjectCodes();
            System.out.println("Fetching subject codes from DB: " + subjectCodes);  // DEBUG
            return subjectCodes;
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();  // Return an empty list on error
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            // Find the ScrollPane with fx:id "contentPane" in the current scene
            contentPane = (ScrollPane) facultyTable.getScene().lookup("#contentPane");

            // If the ScrollPane exists, proceed
            if (contentPane != null) {
                // Create an FXMLLoader to load the FacultyTab.fxml layout
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/pupsis_main_dashboard/fxml/FacultyTab.fxml")
                );

                // Load the FXML content as a Parent node
                Parent newContent = loader.load();

                // Get the controller associated with the loaded FXML (FacultyTabController)
                FacultyTabController controller = loader.getController();

                // Replace the current content of the ScrollPane with the new view
                contentPane.setContent(newContent);
            }
        } catch (IOException e) {
            // Print the stack trace if loading the FXML fails
            e.printStackTrace();
        }
    }

    private void resetScrollPosition() {
        Platform.runLater(() -> {
            contentPane.setVvalue(0);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> contentPane.setVvalue(0));
                }
            }, 100); // 100ms delay for final layout
        });
    }

    @FXML
    private void handleAddFaculty() {
        Faculty newFaculty = showFacultyDialog(null);
        if (newFaculty != null) {
            boolean success = facultyDAO.addFaculty(newFaculty);
            if (success) {
                showAlert("Success", "Faculty added successfully. Click Refresh to update view.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to add faculty.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleEditFaculty() {
        Faculty selected = facultyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Faculty updatedFaculty = showFacultyDialog(selected);
            if (updatedFaculty != null) {
                boolean success = facultyDAO.updateFaculty(updatedFaculty);
                if (success) {
                    showAlert("Success", "Faculty updated. Click Refresh to update view.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to update faculty.", Alert.AlertType.ERROR);
                }
            }
        } else {
            showAlert("Warning", "Please select a faculty member to edit.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteFaculty() {
        Faculty selected = facultyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this record?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait();

            if (confirm.getResult() == ButtonType.YES) {
                boolean success = facultyDAO.deleteFaculty(selected.getFacultyId());
                if (success) {
                    showAlert("Deleted", "Faculty successfully deleted. Click Refresh to update view.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to delete faculty.", Alert.AlertType.ERROR);
                }
            }
        } else {
            showAlert("Warning", "Please select a faculty member to delete.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleRefreshTable() {
        loadFacultyData();
        facultyTable.setItems(facultyList);
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Faculty to CSV");
        fileChooser.setInitialFileName("faculty_export.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(facultyTable.getScene().getWindow());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Faculty ID,First Name,Middle Name,Last Name,Department,Email,Contact,Status,Birthdate,Date Joined\n");
                for (Faculty f : facultyList) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                            escapeCsv(f.getFacultyId()), escapeCsv(f.getFirstName()), escapeCsv(f.getMiddleName()),
                            escapeCsv(f.getLastName()), escapeCsv(f.getDepartment()), escapeCsv(f.getEmail()),
                            escapeCsv(f.getContactNumber()), escapeCsv(f.getStatus()),
                            Objects.toString(f.getBirthdate(), ""), Objects.toString(f.getDateJoined(), "")));
                }
                showAlert("Export Complete", "Faculty data successfully exported.", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Export Failed", "Could not write to file.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handlePrintReport() {
        // Pass the currently displayed items (filtered or full list)
        ObservableList<Faculty> itemsToPrint = facultyTable.getItems();

        PrintableReportController.showPrintableView(new ArrayList<>(itemsToPrint));
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        if (keyword.isEmpty()) {
            facultyTable.setItems(facultyList);  // Reset to full list if empty
            return;
        }

        List<Faculty> filtered = facultyList.stream()
                .filter(f ->
                        (f.getFacultyId() != null && f.getFacultyId().toLowerCase().contains(keyword)) ||
                                (f.getFirstName() != null && f.getFirstName().toLowerCase().contains(keyword)) ||
                                (f.getMiddleName() != null && f.getMiddleName().toLowerCase().contains(keyword)) ||
                                (f.getLastName() != null && f.getLastName().toLowerCase().contains(keyword)) ||
                                (f.getDepartment() != null && f.getDepartment().toLowerCase().contains(keyword)) ||
                                (f.getEmail() != null && f.getEmail().toLowerCase().contains(keyword)) ||
                                (f.getContactNumber() != null && f.getContactNumber().toLowerCase().contains(keyword)) ||
                                (f.getStatus() != null && f.getStatus().toLowerCase().contains(keyword))
                )
                .collect(Collectors.toList());

        facultyTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Faculty showFacultyDialog(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/FacultyRegistrationDialog.fxml"));
            Parent root = loader.load();

            FacultyDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(faculty == null ? "Add Faculty" : "Edit Faculty");
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);
            controller.setFaculty(faculty);

            dialogStage.showAndWait();

            if (controller.isOkClicked()) {
                return controller.getFaculty();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        return text.replace(",", ";").replace("\n", " ");
    }
}