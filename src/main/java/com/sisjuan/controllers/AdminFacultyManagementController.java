package com.sisjuan.controllers;

import com.sisjuan.models.Faculty;
import com.sisjuan.utilities.SubjectDAO;
import com.sisjuan.utilities.FacultyDAO;
import com.sisjuan.utilities.FacultyLoadDAO;
import com.sisjuan.utilities.SectionDAO;
import com.sisjuan.utilities.SchoolYearAndSemester;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.StageStyle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import javafx.geometry.Bounds;


public class AdminFacultyManagementController {

    @FXML private TableView<Faculty> facultyTable;
    @FXML private TableColumn<Faculty, String> idColumn;
    @FXML private TableColumn<Faculty, String> nameColumn;
    @FXML private TableColumn<Faculty, String> departmentColumn;
    @FXML private TableColumn<Faculty, String> emailColumn;
    @FXML private TableColumn<Faculty, String> contactColumn;
    @FXML private TableColumn<Faculty, Void> actionsColumn;
    @FXML private HBox utilityMenuBox;

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

    @FXML
    public void initialize() {
        new Thread(() -> {
            schoolYear = SchoolYearAndSemester.determineCurrentSemester();
            semester = SchoolYearAndSemester.getCurrentAcademicYear();
        }).start();

        facultyDAO = new FacultyDAO();
        facultyLoadDAO = new FacultyLoadDAO();
        subjectDAO = new SubjectDAO();
        sectionDAO = new SectionDAO();

        // === (1) BACKGROUND LOAD FACULTY DATA ===
        Task<List<Faculty>> loadFacultyTask = new Task<>() {
            @Override
            protected List<Faculty> call() throws Exception {
                return facultyDAO.getAllFaculty();
            }
        };

        loadFacultyTask.setOnSucceeded(event -> {
            // This runs on JavaFX thread, safe to update ObservableList
            facultyList.setAll(loadFacultyTask.getValue());
        });

        loadFacultyTask.setOnFailed(event -> {
            Platform.runLater(() -> showAlert(
                    "Database Error",
                    "Failed to load faculty data. " + loadFacultyTask.getException().getMessage(),
                    Alert.AlertType.ERROR
            ));
        });

        facultyTable.setPlaceholder(new Label("Loading faculty data..."));

        new Thread(loadFacultyTask).start();

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

        var columns = new TableColumn[]{idColumn, nameColumn, departmentColumn, emailColumn, contactColumn, actionsColumn};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button menuBtn = new Button("");

            {
                final SVGPath menuIcon = new SVGPath();
                menuIcon.setContent("M12 16a2 2 0 0 1 2 2a2 2 0 0 1-2 2a2 2 0 0 1-2-2a2 2 0 0 1 2-2m0-6a2 2 0 0 1 2 2a2 2 0 0 1-2 2a2 2 0 0 1-2-2a2 2 0 0 1 2-2m0-6a2 2 0 0 1 2 2a2 2 0 0 1-2 2a2 2 0 0 1-2-2a2 2 0 0 1 2-2m0 1a1 1 0 0 0-1 1a1 1 0 0 0 1 1a1 1 0 0 0 1-1a1 1 0 0 0-1-1m0 6a1 1 0 0 0-1 1a1 1 0 0 0 1 1a1 1 0 0 0 1-1a1 1 0 0 0-1-1m0 6a1 1 0 0 0-1 1a1 1 0 0 0 1 1a1 1 0 0 0 1-1a1 1 0 0 0-1-1Z");
                menuIcon.getStyleClass().add("export-menu-bar-icon");
                menuBtn.getStyleClass().add("export-menu-bar");
                menuBtn.setGraphic(menuIcon);
                menuBtn.setTooltip(new Tooltip("More Options"));
                menuBtn.setPrefSize(32, 32);

                menuBtn.setOnAction(event -> {
                    Faculty faculty = getTableView().getItems().get(getIndex());
                    ContextMenu menu = new ContextMenu();

                    // Assign Subject option
                    MenuItem assignItem = new MenuItem("Assign Subject");
                    assignItem.setOnAction(e -> handleAssignSubject(faculty));

                    // Faculty Details option
                    MenuItem detailsItem = new MenuItem("Faculty Details");
                    detailsItem.setOnAction(e -> showFacultyDetailsModal(faculty));

                    // View Assigned Subjects option
                    MenuItem viewAssignmentsItem = new MenuItem("View Assigned Subjects");
                    viewAssignmentsItem.setOnAction(e -> showAssignmentsForFaculty(faculty));

                    // Add menu items
                    menu.getItems().addAll(assignItem, detailsItem, viewAssignmentsItem);

                    // ---- Improved: show menu INSIDE the box ----
                    Bounds btnBounds = menuBtn.localToScreen(menuBtn.getBoundsInLocal());
                    Bounds tableBounds = facultyTable.localToScreen(facultyTable.getBoundsInLocal());

                    double popupX = btnBounds.getMinX();
                    double popupY = btnBounds.getMaxY();
                    double menuWidth = 150; // match your design

                    if (popupX + menuWidth > tableBounds.getMaxX()) {
                        popupX = Math.max(tableBounds.getMinX(), tableBounds.getMaxX() - menuWidth);
                    }

                    menu.getStyleClass().add("export-menu-bar");

                    menu.show(menuBtn, popupX, popupY);

                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : menuBtn);
            }
        });
        // --- Utility menu for export/print ---
        ContextMenu exportPrintMenu = new ContextMenu();

        MenuItem exportItem = new MenuItem("Export CSV");
        exportItem.setOnAction(e -> handleExportCSV());

        MenuItem printItem = new MenuItem("Print");
        printItem.setOnAction(e -> handlePrintReport());

        exportPrintMenu.getItems().addAll(exportItem, printItem);

        // Custom style for this specific menu (add styleClass if needed)
        exportPrintMenu.getStyleClass().add("export-menu-bar");
        exportPrintMenu.setPrefWidth(210);
        exportPrintMenu.setPrefHeight(150);

        utilityMenuBox.setOnMouseClicked(event -> {
            // === BEGIN Smart Positioning ===
            double menuWidth = 115; // Should match your CSS
            Scene scene = utilityMenuBox.getScene();

            // Coordinates of the three-dot HBox in the scene and screen
            Bounds boundsInScene = utilityMenuBox.localToScene(utilityMenuBox.getBoundsInLocal());
            double sceneX = boundsInScene.getMinX();
            double sceneY = boundsInScene.getMinY() + boundsInScene.getHeight();

            double screenX = scene.getWindow().getX() + scene.getX() + sceneX;
            double screenY = scene.getWindow().getY() + scene.getY() + sceneY + 10; // Moved down a little bit

            // App window edges
            double windowRight = scene.getWindow().getX() + scene.getWidth();
            double windowBottom = scene.getWindow().getY() + scene.getHeight();

            // If right edge exceeds a window, shift left
            double menuShowX = screenX;
            if (menuShowX + menuWidth > windowRight - 10) {
                menuShowX = windowRight - menuWidth - 10;
            }

            exportPrintMenu.show(utilityMenuBox, menuShowX, screenY);
            // === END Smart Positioning ===
        });

    }

    private Task<List<Faculty>> getLoadFacultyTask() {
        Task<List<Faculty>> loadFacultyTask = new Task<>() {
            @Override
            protected List<Faculty> call() throws Exception {
                return facultyDAO.getAllFaculty();
            }
        };

        loadFacultyTask.setOnSucceeded(event -> {
            // This runs on JavaFX thread, safe to update ObservableList
            facultyList.setAll(loadFacultyTask.getValue());
        });

        loadFacultyTask.setOnFailed(event -> {
            Platform.runLater(() -> showAlert(
                    "Database Error",
                    "Failed to load faculty data. " + loadFacultyTask.getException().getMessage(),
                    Alert.AlertType.ERROR
            ));
        });
        return loadFacultyTask;
    }


    private void showAssignmentsForFaculty(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/AdminFacultyAssignmentsDialog.fxml"));
            Parent root = loader.load();

            AdminFacultyAssignmentsDialogController controller = loader.getController();
            controller.setFacultyLoadDAO(facultyLoadDAO);
            controller.setFaculty(faculty);
            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            // Optionally show an error dialog
        }
    }


    private void showFacultyDetailsModal(Faculty faculty) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/AdminFacultyDetailsView.fxml"));
            Parent root = loader.load();

            AdminFacultyDetailsController controller = loader.getController();
            controller.setFaculty(faculty);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, Color.TRANSPARENT));
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
    private void handleAssignSubject(Faculty faculty) {
        if (faculty == null) {
            showAlert("Warning", "Please select a faculty member first.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/AdminAssignSubjectDialog.fxml"));
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
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isAssigned()) {
                int facultyId = faculty.getActualFacultyId(); // use parameter
                String subjectCode = controller.getSelectedSubjectCode();
                int sectionId = controller.getSelectedSectionId();

                // Get subject_id (INT) from subject_code (TEXT)
                int subjectId = subjectDAO.getSubjectIdByCode(subjectCode);
                if (subjectId == -1) {
                    showAlert("Error", "Selected subject code not found or invalid.", Alert.AlertType.ERROR);
                    return;
                }

                int semesterId = SchoolYearAndSemester.getCurrentSemesterId();
                int academicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();

                if (semesterId == -1 || academicYearId == -1) {
                    showAlert("Error", "Could not determine current semester or academic year ID.", Alert.AlertType.ERROR);
                    return;
                }

                boolean success = facultyLoadDAO.addFacultyLoad(
                        facultyId,
                        subjectId,
                        sectionId,
                        semesterId,
                        academicYearId
                );

                if (success) {
                    String successFacultyName = faculty.getFirstName() + " " + faculty.getLastName();
                    showAlert("Success", "Subject assigned successfully to " + successFacultyName, Alert.AlertType.INFORMATION);
                    loadFacultyData();
                } else {
                    showAlert(
                            "Assignment already exists!",
                            "You cannot assign the same subject/section/semester/academic year to more than one faculty.",
                            Alert.AlertType.ERROR
                    );
                }

            }
        } catch (IOException e) {
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
                        getClass().getResource("/com/sisjuan/fxml/AdminFacultyPreview.fxml")
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/AdminFacultyRegistrationDialog.fxml"));
            Parent root = loader.load();

            AdminFacultyDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/AdminFacultyRegistrationDialog.fxml"));
            Parent root = loader.load();
            AdminFacultyDialogController controller = loader.getController();
            controller.setFaculty(selectedFaculty);
            controller.setFacultyDAO(facultyDAO);

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root, Color.TRANSPARENT));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/sisjuan/fxml/AdminFacultyRegistrationDialog.fxml"));
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