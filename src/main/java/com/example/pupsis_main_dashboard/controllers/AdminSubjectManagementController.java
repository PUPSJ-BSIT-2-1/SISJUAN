package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.SubjectManagement;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminSubjectManagementController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AdminSubjectManagementController.class);

    @FXML private TableView<SubjectManagement> tableView;
    @FXML private TableColumn<SubjectManagement, String> subjectCodeColumn;
    @FXML private TableColumn<SubjectManagement, String> prerequisiteColumn;
    @FXML private TableColumn<SubjectManagement, String> descriptionColumn;
    @FXML private TableColumn<SubjectManagement, Double> unitColumn;
    @FXML private ComboBox<String> yearSemComboBox;
    @FXML private TextField searchBar;

    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    private final ObservableList<SubjectManagement> allSubjects = FXCollections.observableArrayList();
    private FilteredList<SubjectManagement> filteredSubjects;

    private String currentYearSem = "Year & Semester";
    private String currentSearchText = "";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing AdminSubjectManagementController");

        tableView.setRowFactory(_ -> {
            TableRow<SubjectManagement> row = new TableRow<>();
            row.setPrefHeight(15);
            return row;
        });

        var columns = new TableColumn[]{subjectCodeColumn, prerequisiteColumn, descriptionColumn, unitColumn}; 
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        // Table columns setup
        subjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        prerequisiteColumn.setCellValueFactory(new PropertyValueFactory<>("prerequisite"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // Center align all columns except description
        subjectCodeColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                    setAlignment(Pos.CENTER);
                }
            };
            return cell;
        });

        prerequisiteColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setAlignment(Pos.CENTER);
                    }
                }
            };
            return cell;
        });

        unitColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, Double> cell = new TableCell<SubjectManagement, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                        setAlignment(Pos.CENTER);
                    }

                }
            };
            return cell;
        });

        // Wrap Description Text with center alignment and proper text color handling
        descriptionColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        setAlignment(Pos.CENTER);
                    }
                }
            };
            return cell;
        });

        // ComboBox filter options
        yearSemComboBox.getItems().addAll(
                "Year & Semester",
                "1st Year - 1st Semester",
                "1st Year - 2nd Semester",
                "1st Year - Summer Semester",
                "2nd Year - 1st Semester",
                "2nd Year - 2nd Semester",
                "2nd Year - Summer Semester",
                "3rd Year - 1st Semester",
                "3rd Year - 2nd Semester",
                "3rd Year - Summer Semester",
                "4th Year - 1st Semester",
                "4th Year - 2nd Semester"
        );
        yearSemComboBox.setValue("Year & Semester");

        yearSemComboBox.setOnAction(event -> {
            currentYearSem = yearSemComboBox.getValue();
            updateFilter();
        });

        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchText = newValue;
            updateFilter();
        });

        setupInitialSubjects();

        filteredSubjects = new FilteredList<>(allSubjects, p -> true);
        SortedList<SubjectManagement> sortedSubjects = new SortedList<>(filteredSubjects);
        sortedSubjects.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedSubjects);

        updateFilter();

        // Button handlers
        addButton.setOnAction(_ -> handleAdd());
        editButton.setOnAction(_ -> handleEdit());
        deleteButton.setOnAction(_ -> handleDelete());
        refreshButton.setOnAction(_ -> {
            logger.info("Refresh button clicked.");
            setupInitialSubjects();
            updateFilter();
        });
    }

    private Integer getSemesterIdFromName(String semesterName) {
        logger.debug("Getting semester ID from name: {}", semesterName);
        return switch (semesterName) {
            case "1st Semester" -> 1;
            case "Summer Semester" -> 2;
            case "2nd Semester" -> 3;
            default -> null; // Or throw an exception for an unknown semester name
        };
    }

    private String getSemesterNameFromId(int semesterId) {
        return switch (semesterId) {
            case 1 -> "1st Semester";
            case 2 -> "Summer Semester";
            case 3 -> "2nd Semester";
            default -> "";
        };
    }

    private Integer getYearLevelFromName(String yearName) {
        if (yearName == null || yearName.isEmpty()) return null;
        try {
            return Integer.parseInt(yearName.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getYearLevelNameFromId(int yearLevelId) {
        return switch (yearLevelId) {
            case 1 -> "1st Year";
            case 2 -> "2nd Year";
            case 3 -> "3rd Year";
            case 4 -> "4th Year";
            default -> "";
        };
    }

    private void updateFilter() {
        logger.debug("Updating filter with Year/Sem: '{}' and Search: '{}'", currentYearSem, currentSearchText);
        filteredSubjects.setPredicate(subject -> {
            boolean matchesSearchText = isMatchesSearchText(subject, currentSearchText);

            boolean matchesYearSem = true;
            if (currentYearSem != null && !currentYearSem.equals("Year & Semester")) {
                matchesYearSem = currentYearSem.equals(subject.getYearLevel() + " - " + subject.getSemester());
            }
            return matchesSearchText && matchesYearSem;
        });

        // Put a placeholder if no subjects found
        Label placeholder = new Label("No subjects found");
        placeholder.getStyleClass().add("no-subjects-placeholder");
        tableView.setPlaceholder(placeholder);
    }

    private boolean isMatchesSearchText(SubjectManagement subject, String searchText) {
        logger.debug("Checking if subject matches search text: {}", searchText);
        boolean matchesSearchText = true;
        if (searchText != null && !searchText.isEmpty()) {
            matchesSearchText = subject.getSubjectCode().toLowerCase().contains(searchText) ||
                    subject.getDescription().toLowerCase().contains(searchText) ||
                    (subject.getPrerequisite() != null && subject.getPrerequisite().toLowerCase().contains(searchText));
        }
        return matchesSearchText;
    }

    private void setupInitialSubjects() {
        logger.info("Setting up initial subjects list.");
        allSubjects.clear(); // Clear existing subjects before loading new ones
        String sql = "SELECT subject_id, subject_code, pre_requisites, description, units, year_level, semester_id FROM subjects";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            logger.debug("Executing query to fetch all subjects.");
            while (rs.next()) {
                int yearLevelId = rs.getInt("year_level");
                int semesterId = rs.getInt("semester_id");

                allSubjects.add(new SubjectManagement(
                        rs.getInt("subject_id"),
                        rs.getString("subject_code"),
                        rs.getString("pre_requisites"),
                        rs.getString("description"),
                        rs.getDouble("units"),
                        getYearLevelNameFromId(yearLevelId),
                        getSemesterNameFromId(semesterId)
                ));
            }
            logger.info("Successfully fetched {} subjects from the database.", allSubjects.size());
        } catch (SQLException e) {
            logger.error("Error fetching subjects from database", e);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load subjects from the database.");
        }
        // tableView.setItems(allSubjects); // This will be set via SortedList bound to FilteredList
    }

    // Helper method to show the subject form dialog
    private void showSubjectFormDialog(SubjectManagement subjectToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/AdminSubjectDialog.fxml"));
            Parent root = loader.load();

            AdminSubjectDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, Color.TRANSPARENT));

            // This callback is executed when the form's save button is clicked.
            controller.showForm(subjectToEdit, () -> {
                SubjectManagement resultSubject = controller.getSubject();
                boolean isNew = (subjectToEdit == null);
                if (saveSubjectToDatabase(resultSubject, isNew)) {
                    setupInitialSubjects();
                    updateFilter();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Subject " + (isNew ? "added" : "updated") + " successfully.");
                } else {
                    // Error alert is shown within saveSubjectToDatabase
                }
            });

            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load the subject form FXML.", e);
            showAlert(Alert.AlertType.ERROR, "UI Error", "Could not open the subject form.");
        }
    }

    // Helper method to get the next available subject_id
    private int getNextSubjectId(Connection connection) throws SQLException {
        logger.debug("Getting next available subject ID.");
        // Try to get the next value from a sequence if it exists (PostgreSQL specific)
        try (PreparedStatement stmt = connection.prepareStatement("SELECT nextval('subjects_subject_id_seq')");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Sequence might not exist or other error fallback to MAX(subject_id) + 1
            logger.error("Sequence 'subjects_subject_id_seq' not found or error, falling back to MAX(subject_id): {}", e.getMessage());
        }

        // Fallback: Find the maximum subject_id and increment by 1
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COALESCE(MAX(subject_id), 0) + 1 FROM subjects");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        // Should not happen if a table exists, but as a last resort:
        return 1;
    }

    private boolean saveSubjectToDatabase(SubjectManagement subject, boolean isNewSubject) {
        logger.debug("Saving subject to database: {}", subject.getSubjectCode());
        String sql;
        if (isNewSubject) {
            sql = "INSERT INTO subjects (subject_id, subject_code, pre_requisites, description, units, year_level, semester_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE subjects SET subject_code = ?, pre_requisites = ?, description = ?, units = ?, year_level = ?, semester_id = ? WHERE subject_id = ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Integer yearLevel = getYearLevelFromName(subject.getYearLevel());
            Integer semesterId = getSemesterIdFromName(subject.getSemester());

            if (isNewSubject) {
                int newSubjectId = getNextSubjectId(conn);
                pstmt.setInt(1, newSubjectId); // Use generated/retrieved subject_id
                pstmt.setString(2, subject.getSubjectCode());
                pstmt.setString(3, subject.getPrerequisite());
                pstmt.setString(4, subject.getDescription());
                pstmt.setDouble(5, subject.getUnit());
                pstmt.setInt(6, yearLevel != null ? yearLevel : 0);
                pstmt.setInt(7, semesterId != null ? semesterId : 0);
            } else {
                pstmt.setString(1, subject.getSubjectCode());
                pstmt.setString(2, subject.getPrerequisite());
                pstmt.setString(3, subject.getDescription());
                pstmt.setDouble(4, subject.getUnit());
                pstmt.setInt(5, yearLevel != null ? yearLevel : 0);
                pstmt.setInt(6, semesterId != null ? semesterId : 0);
                pstmt.setInt(7, subject.getSubjectId()); // WHERE clause
            }

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            logger.error("Error saving subject to database: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", (isNewSubject ? "Failed to add subject: " : "Failed to update subject: ") + e.getMessage());
            return false;
        }
    }

    private void handleAdd() {
        logger.info("Add button clicked.");
        showSubjectFormDialog(null);
    }

    private void handleEdit() {
        SubjectManagement selectedSubject = tableView.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a subject to edit.");
            logger.warn("Edit button clicked without a selection.");
            return;
        }

        logger.info("Edit button clicked for subject: {}", selectedSubject.getSubjectCode());
        showSubjectFormDialog(selectedSubject);
    }

    private void handleDelete() {
        SubjectManagement selectedSubject = tableView.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a subject to delete.");
            logger.warn("Delete button clicked without a selection.");
            return;
        }

        logger.info("Delete button clicked for subject: {}", selectedSubject.getSubjectCode());
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Confirm Deletion");
        confirmationDialog.setHeaderText("Delete Subject: " + selectedSubject.getSubjectCode());
        confirmationDialog.setContentText("Are you sure you want to delete this subject? This action cannot be undone.");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logger.debug("User confirmed deletion for subject: {}.", selectedSubject.getSubjectCode());
            if (deleteSubjectFromDatabase(selectedSubject.getSubjectCode())) {
                setupInitialSubjects(); // Refresh list from DB
                updateFilter();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Subject deleted successfully.");
            }
        }
    }

    private boolean deleteSubjectFromDatabase(String subjectCode) {
        logger.info("Attempting to delete subject: {}", subjectCode);
        String sql = "DELETE FROM subjects WHERE subject_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, subjectCode);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Successfully deleted subject: {}", subjectCode);
                return true;
            } else {
                logger.warn("Deletion failed for subject '{}', it might not exist.", subjectCode);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete the subject. It might have already been deleted or does not exist.");
                return false;
            }
        } catch (SQLException e) {
            logger.error("SQL error while deleting subject: {}", subjectCode, e);
            // Check for foreign key constraint violation (e.g., if the subject is in use)
            if (e.getSQLState().equals("23503")) { // PostgreSQL specific error code for foreign_key_violation
                showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Cannot delete subject: It is currently assigned or referenced by other records (e.g., faculty load, student grades). Please remove those associations first.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while deleting the subject.");
            }
            return false;
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}