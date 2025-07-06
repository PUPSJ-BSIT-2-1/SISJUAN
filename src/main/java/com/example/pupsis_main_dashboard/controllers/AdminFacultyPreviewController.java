package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Faculty;
import com.example.pupsis_main_dashboard.utilities.FacultyDAO;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.control.Tooltip;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminFacultyPreviewController {

    @FXML private Label totalFacultyLabel;
    @FXML private Label fullTimeFacultyLabel;
    @FXML private Label partTimeFacultyLabel;
    @FXML private Label currentDateLabel;
    @FXML private Button manageButton;

    @FXML private TextField searchField;
    @FXML private TableView<Faculty> recentFacultyTable;
    @FXML private HBox fullTimeContainer;
    @FXML private HBox partTimeContainer;
    @FXML private TableColumn<Faculty, String> idColumn;
    @FXML private TableColumn<Faculty, String> nameColumn;
    @FXML private TableColumn<Faculty, String> deptColumn;
    @FXML private TableColumn<Faculty, String> joinedColumn;
    private FacultyDAO facultyDAO;

    @FXML
    private void initialize() {
        facultyDAO = new FacultyDAO();

        // Set the action for the "Manage Faculty" button
        manageButton.setOnAction(this::loadFacultyTab);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("facultyId"));
        nameColumn.setCellValueFactory(cellData -> {
            Faculty f = cellData.getValue();
            return new SimpleStringProperty(f.getFirstName() + " " + f.getLastName());
        });
        deptColumn.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        joinedColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDateJoined();
            return new SimpleStringProperty(date != null ? date.toString() : "");
        });


        var columns = new TableColumn[]{idColumn, nameColumn, deptColumn, joinedColumn};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTable(newVal));

        Tooltip.install(totalFacultyLabel, new Tooltip("Total number of faculty members."));
        Tooltip.install(fullTimeContainer, new Tooltip("Number of full-time faculty members."));
        Tooltip.install(partTimeContainer, new Tooltip("Number of part-time faculty members."));
        Tooltip.install(currentDateLabel, new Tooltip("Current system date."));
        Tooltip.install(searchField, new Tooltip("Search by faculty ID, name or department"));

        loadDashboardData();
    }

    @FXML
    private void loadFacultyTab(ActionEvent event) {
        try {
            // Locate the ScrollPane with fx:id "contentPane" in the current scene
            ScrollPane contentPane = (ScrollPane) recentFacultyTable.getScene().lookup("#contentPane");

            // If the ScrollPane is found, proceed with loading new content
            if (contentPane != null) {
                // Create an FXMLLoader to load the AdminFacultyManagement.fxml layout
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminFacultyManagement.fxml")
                );

                // Load the FXML content into a Parent node
                Parent newContent = loader.load();

                // Retrieve the controller for the loaded FXML (AdminFacultyManagementController)
                AdminFacultyManagementController controller = loader.getController();

                // Replace the current content of the ScrollPane with the newly loaded content
                contentPane.setContent(newContent);
            }
        } catch (IOException e) {
            // Print error details in case the FXML fails to load
            e.printStackTrace();
        }
    }

    private void loadDashboardData() {
        try {
            List<Faculty> allFaculty = facultyDAO.getAllFaculty();
            int total = allFaculty.size();
            long fullTimeCount = allFaculty.stream()
                    .filter(f -> f.getFacultyStatusName() != null && "Full-time".equalsIgnoreCase(f.getFacultyStatusName()))
                    .count();
            long partTimeCount = allFaculty.stream()
                    .filter(f -> f.getFacultyStatusName() != null && "Part-time".equalsIgnoreCase(f.getFacultyStatusName()))
                    .count();

            totalFacultyLabel.setText("Total Faculty: " + total);
            fullTimeFacultyLabel.setText("Full-Time: " + fullTimeCount);
            partTimeFacultyLabel.setText("Part-Time: " + partTimeCount);
            currentDateLabel.setText("Date: " + LocalDate.now());

            List<Faculty> recent = allFaculty.stream()
                    .sorted(Comparator.comparing(
                            Faculty::getDateJoined,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            recentFacultyTable.setItems(FXCollections.observableArrayList(recent));
        } catch (Exception e) {
            e.printStackTrace();
            StageAndSceneUtils.showAlert("Error", "Failed to load dashboard data.", Alert.AlertType.ERROR);
        }
    }

    private void filterTable(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            loadDashboardData();
            return;
        }

        try {
            List<Faculty> all = facultyDAO.getAllFaculty();
            List<Faculty> filtered = all.stream()
                    .filter(f ->
                            (f.getFirstName() != null && f.getFirstName().toLowerCase().contains(keyword.toLowerCase())) ||
                                    (f.getLastName() != null && f.getLastName().toLowerCase().contains(keyword.toLowerCase())) ||
                                    (f.getDepartmentName() != null && f.getDepartmentName().toLowerCase().contains(keyword.toLowerCase())) ||
                                    (f.getFacultyId() != null && f.getFacultyId().toLowerCase().contains(keyword.toLowerCase()))
                    )
                    .collect(Collectors.toList());
            recentFacultyTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (Exception e) {
            StageAndSceneUtils.showAlert("Error", "Failed to filter table.", Alert.AlertType.ERROR);
        }
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
