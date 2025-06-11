package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Faculty;
import com.example.pupsis_main_dashboard.utilities.SubjectDAO;
import com.example.pupsis_main_dashboard.utilities.FacultyDAO;
import com.example.pupsis_main_dashboard.utilities.FacultyLoadDAO;
import com.example.pupsis_main_dashboard.utilities.SectionDAO;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
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

public class AdminFacultyManagementController {

    @FXML private TableView<Faculty> facultyTable;
    @FXML private TableColumn<Faculty, String> idColumn;
    @FXML private TableColumn<Faculty, String> nameColumn;
    @FXML private TableColumn<Faculty, String> departmentColumn;
    @FXML private TableColumn<Faculty, String> emailColumn;
    @FXML private TableColumn<Faculty, String> contactColumn;
    @FXML private TableColumn<Faculty, Void> detailsColumn;
    @FXML private HBox backButton;
    @FXML private TextField searchField;

    private final ObservableList<Faculty> facultyList = FXCollections.observableArrayList();
    private FacultyDAO facultyDAO;
    private FacultyLoadDAO facultyLoadDAO;
    private SubjectDAO subjectDAO;
    private SectionDAO sectionDAO;
    private ScrollPane contentPane;
    private String schoolYear;
    private String semester;

    public void initialize() {
        new Thread(() -> {
            schoolYear = SchoolYearAndSemester.determineCurrentSemester();
            semester = SchoolYearAndSemester.getCurrentAcademicYear();
        }).start();

        try {
            facultyDAO = new FacultyDAO();
            loadFacultyData();
            facultyLoadDAO = new FacultyLoadDAO();
            subjectDAO = new SubjectDAO();
            sectionDAO = new SectionDAO();
        } catch (SQLException e) {
            Platform.runLater(() -> showAlert("Database Error", "Failed to connect to the database.", Alert.AlertType.ERROR));
        }


        // Go back to the dashboard when the back button is clicked
        backButton.setOnMouseClicked(_ -> {
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
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());

        facultyTable.setItems(facultyList);
        detailsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("➕");

            {
                btn.getStyleClass().add("details-button");
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

        var columns = new TableColumn[]{idColumn, nameColumn, departmentColumn, emailColumn, contactColumn, detailsColumn};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }
    }

    private void showFacultyDetailsModal(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminFacultyDetailsTableView.fxml"));
            Parent root = loader.load();

            AdminFacultyDetailsController controller = loader.getController();
            controller.setFaculty(faculty);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Faculty Details - " + faculty.getFirstName() + " " + faculty.getLastName());
            stage.setScene(new Scene(root));
            controller.setDialogStage(stage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open faculty details window.", Alert.AlertType.ERROR);
        }
    }


    public void loadFacultyData() {
        facultyList.clear();
        try {
            List<Faculty> faculties = facultyDAO.getAllFaculty();
            facultyList.addAll(faculties);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminAssignSubjectDialog.fxml"));
            Parent root = loader.load();

            AdminAssignSubjectDialogController controller = loader.getController();

            // Load subjects
            List<String> subjectCodes = subjectDAO.getAllSubjectCodes();
            if (subjectCodes.isEmpty()) {
                showAlert("Information", "No subjects available to assign.", Alert.AlertType.INFORMATION);
                return;
            }
            controller.setSubjects(subjectCodes);

            // Load sections using SectionDAO
            List<AdminAssignSubjectDialogController.SectionItem> sectionItems = sectionDAO.getAllSectionItems();
            if (sectionItems.isEmpty()) {
                showAlert("Information", "No sections available to assign.", Alert.AlertType.INFORMATION);
                return;
            }
            controller.setSections(sectionItems);

            controller.setSchoolYearAndSemester(schoolYear, semester);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            String facultyFullName = selectedFaculty.getFirstName() + " " + selectedFaculty.getLastName();
            dialogStage.setTitle("Assign Subject to " + facultyFullName);
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isAssigned()) {
                int facultyId = selectedFaculty.getActualFacultyId();
                String subjectCode = controller.getSelectedSubjectCode();
                int sectionId = controller.getSelectedSectionId();

                // Get subject_id (INT) from subject_code (TEXT)
                int subjectId = subjectDAO.getSubjectIdByCode(subjectCode);
                if (subjectId == -1) {
                    showAlert("Error", "Selected subject code not found or invalid.", Alert.AlertType.ERROR);
                    return;
                }

                // Get current semester and academic year IDs
                int semesterId = SchoolYearAndSemester.getCurrentSemesterId();
                int academicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();

                if (semesterId == -1 || academicYearId == -1) {
                    showAlert("Error", "Could not determine current semester or academic year ID.", Alert.AlertType.ERROR);
                    return;
                }

                for (FacultyLoadDAO.FacultyLoad load : facultyLoadDAO.getAllFacultyLoad()) {
                    if (load.facultyId() == facultyId && load.subjectId() == subjectId && load.sectionId() == sectionId && load.semesterId() == semesterId && load.academicYearId() == academicYearId) {
                        showAlert("Error", "Subject is already assigned to this faculty member.", Alert.AlertType.ERROR);
                        return;
                    }
                }

                boolean success = facultyLoadDAO.addFacultyLoad(
                        facultyId,
                        subjectId,
                        sectionId,
                        semesterId,
                        academicYearId
                );

                if (success) {
                    String successFacultyName = selectedFaculty.getFirstName() + " " + selectedFaculty.getLastName();
                    showAlert("Success", "Subject assigned successfully to " + successFacultyName, Alert.AlertType.INFORMATION);
                    loadFacultyData();
                } else {
                    showAlert("Error", "Failed to assign subject. Check logs for details.", Alert.AlertType.ERROR);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading AdminAssignSubjectDialog.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Could not open the assign subject dialog.", Alert.AlertType.ERROR);
        } catch (SQLException se) {
            System.err.println("Database error during subject assignment: " + se.getMessage());
            se.printStackTrace();
            showAlert("Database Error", "A database error occurred: " + se.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            // Find the ScrollPane with fx:id "contentPane" in the current scene
            contentPane = (ScrollPane) facultyTable.getScene().lookup("#contentPane");

            if (contentPane != null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminFacultyPreview.fxml")
                );

                Parent newContent = loader.load();

                AdminFacultyPreviewController controller = loader.getController();

                contentPane.setContent(newContent);
            }
        } catch (IOException e) {
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminFacultyRegistrationDialog.fxml"));
            Parent root = loader.load();

            AdminFacultyDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Add New Faculty");
            dialogStage.setScene(new Scene(root));
            controller.setFacultyDAO(facultyDAO);
            controller.setDialogStage(dialogStage);
            controller.setFaculty(null); // Pass null for a new faculty

            dialogStage.showAndWait();

            // Only reload table if user saved successfully!
            if (controller.isSaveClicked()) {
                showAlert("Success", "Click Refresh to update the list.", Alert.AlertType.INFORMATION);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open the add faculty window.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEditFaculty() {
        Faculty selectedFaculty = facultyTable.getSelectionModel().getSelectedItem();
        if (selectedFaculty == null) {
            showAlert("No Selection", "Please select a faculty member to edit.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminFacultyRegistrationDialog.fxml"));
            Parent root = loader.load();
            AdminFacultyDialogController controller = loader.getController();
            controller.setFaculty(selectedFaculty);
            controller.setFacultyDAO(facultyDAO);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Faculty");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) { 
                showAlert("Success", "Click Refresh to update the list.", Alert.AlertType.INFORMATION);
            } else {
                if (controller.getErrorMessage() != null && !controller.getErrorMessage().isEmpty()) {
                    showAlert("Error", controller.getErrorMessage(), Alert.AlertType.ERROR);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open edit dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }



    @FXML
    private void handleDeleteFaculty() {
        Faculty selectedFaculty = facultyTable.getSelectionModel().getSelectedItem();
        if (selectedFaculty == null) {
            showAlert("No Selection", "Please select a faculty member to delete.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Are you sure you want to delete this faculty member?");
        confirm.setContentText("This action cannot be undone.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        int result = facultyDAO.deleteFaculty(selectedFaculty.getActualFacultyId());
        if (result == 1) {
            showAlert("Success", "Faculty member deleted successfully.", Alert.AlertType.INFORMATION);
            loadFacultyData(); // Refresh table
        } else if (result == -1) {
            showAlert("Delete Failed",
                    "This faculty member cannot be deleted because they are assigned to faculty loads.\n"
                            + "Remove all their assignments first before deleting.",
                    Alert.AlertType.ERROR);
        } else {
            showAlert("Error", "Failed to delete faculty. Please try again.", Alert.AlertType.ERROR);
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
                            escapeCsv(f.getLastName()), escapeCsv(f.getDepartmentName()), escapeCsv(f.getEmail()),
                            escapeCsv(f.getContactNumber()), escapeCsv(f.getFacultyStatusName()),
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
        ObservableList<Faculty> itemsToPrint = facultyTable.getItems();

        AdminPrintableReportController.showPrintableView(new ArrayList<>(itemsToPrint));
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        if (keyword.isEmpty()) {
            facultyTable.setItems(facultyList);
        } else {
            ObservableList<Faculty> filteredList = facultyList.stream()
                    .filter(faculty -> faculty.getFirstName().toLowerCase().contains(keyword) ||
                            faculty.getLastName().toLowerCase().contains(keyword) ||
                            faculty.getFacultyId().toLowerCase().contains(keyword) ||
                            (faculty.getDepartmentName() != null && faculty.getDepartmentName().toLowerCase().contains(keyword)) ||
                            (faculty.getEmail() != null && faculty.getEmail().toLowerCase().contains(keyword)) ||
                            (faculty.getContactNumber() != null && faculty.getContactNumber().toLowerCase().contains(keyword)) ||
                            (faculty.getFacultyStatusName() != null && faculty.getFacultyStatusName().toLowerCase().contains(keyword)))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            facultyTable.setItems(filteredList);
        }
    }

    private Faculty showFacultyDialog(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminFacultyRegistrationDialog.fxml"));
            Parent root = loader.load();

            AdminFacultyDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(faculty == null ? "Add Faculty" : "Edit Faculty");
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);
            controller.setFaculty(faculty);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
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