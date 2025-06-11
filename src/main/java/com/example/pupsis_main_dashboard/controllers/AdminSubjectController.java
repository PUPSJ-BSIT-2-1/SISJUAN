package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class AdminSubjectController implements Initializable {

    @FXML private TableView<SubjectManagement> tableView;
    @FXML private TableColumn<SubjectManagement, String> subjectCodeColumn;
    @FXML private TableColumn<SubjectManagement, String> prerequisiteColumn;
    @FXML private TableColumn<SubjectManagement, String> descriptionColumn;
    @FXML private TableColumn<SubjectManagement, Double> unitColumn;
    @FXML private TableColumn<SubjectManagement, String> equivSubjectCodeColumn; // Added equivSubjectCodeColumn
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

        tableView.setRowFactory(_ -> {
            TableRow<SubjectManagement> row = new TableRow<>();
            row.setPrefHeight(15);
            return row;
        });

        var columns = new TableColumn[]{subjectCodeColumn, prerequisiteColumn, descriptionColumn, unitColumn, equivSubjectCodeColumn}; // Added equivSubjectCodeColumn
        for (var col : columns) {
            col.setReorderable(false);
        }

        for (var col : columns) {
            col.setSortable(false);
        }

        // Table columns setup
        subjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        prerequisiteColumn.setCellValueFactory(new PropertyValueFactory<>("prerequisite"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        equivSubjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("equivSubjectCode")); // Setup for equivSubjectCodeColumn

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

        equivSubjectCodeColumn.setCellFactory(tc -> {
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
            setupInitialSubjects();
            updateFilter();
        });
    }

    private Integer getSemesterIdFromName(String semesterName) {
        return switch (semesterName) {
            case "1st Semester" -> 1;
            case "Summer Semester" -> 2;
            case "2nd Semester" -> 3;
            default -> null; // Or throw an exception for an unknown semester name
        };
    }

    private void updateFilter() {
        String searchText = searchBar.getText().toLowerCase();
        String selectedYearSem = yearSemComboBox.getValue();

        filteredSubjects.setPredicate(subject -> {
            boolean matchesSearchText = isMatchesSearchText(subject, searchText);

            boolean matchesYearSem = true;
            if (selectedYearSem != null && !selectedYearSem.equals("Year & Semester")) {
                String[] parts = selectedYearSem.split(" - ");
                if (parts.length == 2) {
                    try {
                        int yearFilter = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                        String semesterNameFilter = parts[1];
                        Integer semesterIdFilter = getSemesterIdFromName(semesterNameFilter);

                        matchesYearSem = subject.getYearLevel() != null && subject.getYearLevel() == yearFilter &&
                                subject.getSemesterId() != null && subject.getSemesterId().equals(semesterIdFilter);
                    } catch (NumberFormatException e) {
                        // Handle parsing error if necessary, for now, assume a valid format or no match
                        matchesYearSem = false;
                    }
                } else {
                    matchesYearSem = false; // Invalid format
                }
            }
            return matchesSearchText && matchesYearSem;
        });

        // Put a placeholder if no subjects found
        Label placeholder = new Label("No subjects found");
        placeholder.getStyleClass().add("no-subjects-placeholder");
        tableView.setPlaceholder(placeholder);
    }

    private boolean isMatchesSearchText(SubjectManagement subject, String searchText) {
        boolean matchesSearchText = true;
        if (searchText != null && !searchText.isEmpty()) {
            matchesSearchText = subject.getSubjectCode().toLowerCase().contains(searchText) ||
                    subject.getDescription().toLowerCase().contains(searchText) ||
                    (subject.getPrerequisite() != null && subject.getPrerequisite().toLowerCase().contains(searchText)) ||
                    (subject.getEquivSubjectCode() != null && subject.getEquivSubjectCode().toLowerCase().contains(searchText)); // Added equivSubjectCode to filter
        }
        return matchesSearchText;
    }

    private void setupInitialSubjects() {
        allSubjects.clear(); // Clear existing subjects before loading new ones
        String sql = "SELECT subject_id, subject_code, pre_requisites, description, units, year_level, semester_id FROM subjects";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                allSubjects.add(new SubjectManagement(
                        rs.getInt("subject_id"),
                        rs.getString("subject_code"),
                        rs.getString("pre_requisites"),
                        rs.getString("description"),
                        rs.getDouble("units"),
                        rs.getInt("year_level"),
                        rs.getInt("semester_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Consider more sophisticated error handling
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load subjects from the database.");
        }
        // tableView.setItems(allSubjects); // This will be set via SortedList bound to FilteredList
    }

    // Helper method to show the subject form dialog
    private SubjectManagement showSubjectFormDialog(SubjectManagement subjectToEdit) {
        // TODO: Implement or locate AdminSubjectForm.fxml and its controller
        // Currently, the FXML and/or its controller cannot be found, so this functionality is disabled.
        showAlert(Alert.AlertType.WARNING, "Feature Unavailable", "The form for adding/editing subjects is currently unavailable.");
        return null; // Return null as the form cannot be shown
    }

    // Helper method to get the next available subject_id
    private int getNextSubjectId(Connection connection) throws SQLException {
        // Try to get the next value from a sequence if it exists (PostgreSQL specific)
        try (PreparedStatement stmt = connection.prepareStatement("SELECT nextval('subjects_subject_id_seq')");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Sequence might not exist or other error fallback to MAX(subject_id) + 1
            System.err.println("Sequence 'subjects_subject_id_seq' not found or error, falling back to MAX(subject_id): " + e.getMessage());
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
        String sql;
        if (isNewSubject) {
            sql = "INSERT INTO subjects (subject_id, subject_code, pre_requisites, description, units, year_level, semester_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE subjects SET subject_code = ?, pre_requisites = ?, description = ?, units = ?, year_level = ?, semester_id = ? WHERE subject_id = ?";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (isNewSubject) {
                int newSubjectId = getNextSubjectId(conn);
                pstmt.setInt(1, newSubjectId); // Use generated/retrieved subject_id
                pstmt.setString(2, subject.getSubjectCode());
                pstmt.setString(3, subject.getPrerequisite());
                pstmt.setString(4, subject.getDescription());
                pstmt.setDouble(5, subject.getUnit());
                pstmt.setInt(6, subject.getYearLevel()); // Direct integer year_level
                pstmt.setInt(7, subject.getSemesterId()); // Direct integer semester_id
            } else {
                pstmt.setString(1, subject.getSubjectCode());
                pstmt.setString(2, subject.getPrerequisite());
                pstmt.setString(3, subject.getDescription());
                pstmt.setDouble(4, subject.getUnit());
                pstmt.setInt(5, subject.getYearLevel()); // Direct integer year_level
                pstmt.setInt(6, subject.getSemesterId()); // Direct integer semester_id
                pstmt.setInt(7, subject.getSubjectId()); // WHERE clause
            }

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", (isNewSubject ? "Failed to add subject: " : "Failed to update subject: ") + e.getMessage());
            return false;
        }
    }

    private void handleAdd() {
        SubjectManagement newSubject = showSubjectFormDialog(null);
        if (newSubject != null) {
            if (saveSubjectToDatabase(newSubject, true)) {
                // If DB save is successful, refresh the list from DB to get the new ID if it was auto-generated by a sequence
                setupInitialSubjects();
                updateFilter();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Subject added successfully.");
            } else {
                // showAlert is already called in saveSubjectToDatabase on failure
            }
        }
    }

    private void handleEdit() {
        SubjectManagement selectedSubject = tableView.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a subject to edit.");
            return;
        }

        SubjectManagement editedSubject = showSubjectFormDialog(selectedSubject);
        if (editedSubject != null) {
            // The editedSubject might be a new instance from the form, ensure it has the original ID for update
            SubjectManagement subjectToSave = new SubjectManagement(
                    selectedSubject.getSubjectId(), // Crucial: use original ID for update
                    editedSubject.getSubjectCode(),
                    editedSubject.getPrerequisite(),
                    editedSubject.getDescription(),
                    editedSubject.getUnit(),
                    editedSubject.getYearLevel(),
                    editedSubject.getSemesterId()
            );

            if (saveSubjectToDatabase(subjectToSave, false)) {
                setupInitialSubjects(); // Refresh list from DB
                updateFilter();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Subject updated successfully.");
            } else {
                // showAlert is already called in saveSubjectToDatabase on failure
            }
        }
    }

    private void handleDelete() {
        SubjectManagement selectedSubject = tableView.getSelectionModel().getSelectedItem();
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a subject to delete.");
            return;
        }

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Confirm Deletion");
        confirmationDialog.setHeaderText("Delete Subject: " + selectedSubject.getSubjectCode());
        confirmationDialog.setContentText("Are you sure you want to delete this subject? This action cannot be undone.");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM subjects WHERE subject_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, selectedSubject.getSubjectId());
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Subject deleted successfully.");
                    allSubjects.remove(selectedSubject); // Remove from the observable list
                    updateFilter(); // Refresh the table view
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete the subject. It might have already been deleted or does not exist.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Check for foreign key constraint violation (e.g., if the subject is in use)
                if (e.getSQLState().equals("23503")) { // PostgreSQL specific error code for foreign_key_violation
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Cannot delete subject: It is currently assigned or referenced by other records (e.g., faculty load, student grades). Please remove those associations first.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while trying to delete the subject: " + e.getMessage());
                }
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for Subject data model
    public static class SubjectManagement {
        private final Integer subjectId;
        private final String subjectCode;
        private final String prerequisite;
        private final String description;
        private final Double unit;
        private final Integer yearLevel; // Changed from yearLevelId
        private final Integer semesterId;
        private final String equivSubjectCode; // Added equivSubjectCode

        public SubjectManagement(Integer subjectId, String subjectCode, String prerequisite, String description, Double unit, Integer yearLevel, Integer semesterId) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.prerequisite = prerequisite;
            this.description = description;
            this.unit = unit;
            this.yearLevel = yearLevel;
            this.semesterId = semesterId;
            this.equivSubjectCode = subjectCode; // Copy subjectCode to equivSubjectCode
        }

        // Getters
        public Integer getSubjectId() { return subjectId; }
        public String getSubjectCode() { return subjectCode; }
        public String getPrerequisite() { return prerequisite; }
        public String getDescription() { return description; }
        public Double getUnit() { return unit; }
        public Integer getYearLevel() { return yearLevel; }
        public Integer getSemesterId() { return semesterId; }
        public String getEquivSubjectCode() { return equivSubjectCode; } // Getter for equivSubjectCode
    }
}