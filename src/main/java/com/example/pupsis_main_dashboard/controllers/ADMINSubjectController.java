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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class ADMINSubjectController implements Initializable {

    @FXML private TableView<SubjectManagement> tableView;
    @FXML private TableColumn<SubjectManagement, String> subjectCodeColumn;
    @FXML private TableColumn<SubjectManagement, String> prerequisiteColumn;
    @FXML private TableColumn<SubjectManagement, String> equivSubjectCodeColumn;
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
        // Table columns setup
        subjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        prerequisiteColumn.setCellValueFactory(new PropertyValueFactory<>("prerequisite"));
        equivSubjectCodeColumn.setCellValueFactory(new PropertyValueFactory<>("equivSubjectCode"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

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
                    }
                    setAlignment(Pos.CENTER);
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
                    }
                    setAlignment(Pos.CENTER);
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
                    }
                    setAlignment(Pos.CENTER);
                }
            };
            return cell;
        });

        // Wrap Description Text with center alignment and proper text color handling
        descriptionColumn.setCellFactory(tc -> {
            TableCell<SubjectManagement, String> cell = new TableCell<SubjectManagement, String>() {
                private final Text text = new Text();

                {
                    text.wrappingWidthProperty().bind(descriptionColumn.widthProperty().subtract(10));
                    setGraphic(text);
                    setAlignment(Pos.CENTER);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        text.setText(null);
                    } else {
                        text.setText(item);
                    }

                    // Handle text color for hover state
                    TableRow<?> row = getTableRow();
                    if (row != null) {
                        row.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                            if (isNowHovered) {
                                text.setStyle("-fx-fill: white;");
                            } else {
                                text.setStyle("-fx-fill: black;");
                            }
                        });

                        // Set initial color based on current hover state
                        if (row.isHover()) {
                            text.setStyle("-fx-fill: white;");
                        } else {
                            text.setStyle("-fx-fill: black;");
                        }
                    }
                }
            };
            return cell;
        });

        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

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
        addButton.setOnAction(e -> handleAdd());
        editButton.setOnAction(e -> handleEdit());
        deleteButton.setOnAction(e -> handleDelete());
        refreshButton.setOnAction(e -> {
            setupInitialSubjects();
            updateFilter();
        });
    }

    private void setupInitialSubjects() {
        // Clear existing subjects if any
        allSubjects.clear();

        // Fetch subjects from Supabase
        try (Connection connection = DBConnection.getConnection()) {
            String query = "SELECT s.subject_code, s.pre_requisites, s.description, s.units, yl.year_level_name AS year_level, sem.semester_name AS semester " +
                           "FROM public.subjects s " +
                           "JOIN public.year_levels yl ON s.year_level_id = yl.year_level_id " +
                           "JOIN public.semesters sem ON s.semester_id = sem.semester_id " +
                           "ORDER BY yl.year_level_name, sem.semester_name, s.subject_code";

            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String subjectCode = resultSet.getString("subject_code");
                    String preRequisites = resultSet.getString("pre_requisites");
                    String description = resultSet.getString("description");
                    double units = resultSet.getDouble("units");
                    String yearLevel = resultSet.getString("year_level"); // Already aliased from yl.year_level_name
                    String semester = resultSet.getString("semester"); // Now aliased from sem.semester_name

                    // Using subjectCode for equivSubjectCode as specified
                    SubjectManagement subject = new SubjectManagement(
                            subjectCode,
                            preRequisites != null ? preRequisites : "",
                            subjectCode, // Using subject_code for equiv_code
                            description,
                            units,
                            yearLevel,
                            semester
                    );

                    allSubjects.add(subject);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load subjects from the database: " + e.getMessage());
        }
    }



    private void updateFilter() {
        filteredSubjects.setPredicate(subject -> {
            boolean yearSemMatch = true;

            if (!"Year & Semester".equals(currentYearSem)) {
                if (currentYearSem.contains("SUMMER")) {
                    yearSemMatch = "SUMMER".equalsIgnoreCase(subject.getSemester());
                } else {
                    String[] parts = currentYearSem.split(" - ");
                    if (parts.length == 2) {
                        String year = parts[0].trim();
                        String semester = parts[1].trim();
                        yearSemMatch = year.equalsIgnoreCase(subject.getYearLevel()) && semester.equalsIgnoreCase(subject.getSemester());
                    }
                }
            }

            boolean searchMatch = true;
            if (currentSearchText != null && !currentSearchText.isEmpty()) {
                String searchTextLower = currentSearchText.toLowerCase();
                searchMatch = subject.getSubjectCode().toLowerCase().contains(searchTextLower) ||
                        subject.getDescription().toLowerCase().contains(searchTextLower) ||
                        subject.getPrerequisite().toLowerCase().contains(searchTextLower) ||
                        subject.getEquivSubjectCode().toLowerCase().contains(searchTextLower);
            }

            return yearSemMatch && searchMatch;
        });
    }

    private void handleAdd() {
        SubjectManagement newSubject = showSubjectForm(null);
        if (newSubject != null) {
            // Save to database first
            if (saveSubjectToDatabase(newSubject, true)) {
                // If database save was successful, add to UI
                allSubjects.add(newSubject);
                updateFilter();
                showAlert("Success", "Subject added successfully.");
            }
        }
    }

    private void handleEdit() {
        SubjectManagement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SubjectManagement updatedSubject = showSubjectForm(selected);
            if (updatedSubject != null) {
                // Save changes to database
                if (saveSubjectToDatabase(updatedSubject, false)) {
                    // Refresh the table
                    setupInitialSubjects(); // Reload all subjects
                    tableView.refresh();
                    showAlert("Success", "Subject updated successfully.");
                }
            }
        } else {
            showAlert("Edit Failed", "Please select a subject to edit.");
        }
    }

    private void handleDelete() {
        SubjectManagement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Confirm deletion
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Are you sure you want to delete the subject: " + selected.getSubjectCode() + "?");
        
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Delete from database first
                if (deleteSubjectFromDatabase(selected)) {
                    // If database deletion was successful, remove from UI
                    allSubjects.remove(selected);
                    updateFilter();
                    showAlert("Success", "Subject deleted successfully.");
                }
            }
        } else {
            showAlert("Delete Failed", "Please select a subject to delete.");
        }
    }

    private SubjectManagement showSubjectForm(SubjectManagement subject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/ADMINSubjectForm.fxml"));
            Parent parent = loader.load();

            ADMINSubjectFormController controller = loader.getController();
            if (subject != null) {
                controller.setSubject(subject);
            }

            Stage stage = new Stage();
            stage.setTitle(subject == null ? "Add Subject" : "Edit Subject");
            stage.setScene(new Scene(parent));
            stage.setResizable(false);
            stage.showAndWait();

            return controller.getSubject();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Form Load Error", "Unable to open the subject form: " + e.getMessage());
            return null;
        }
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void addSubject(SubjectManagement subject) {
        allSubjects.add(subject);
        updateFilter();
    }

private boolean saveSubjectToDatabase(SubjectManagement subject, boolean isNewSubject) {
    try (Connection connection = DBConnection.getConnection()) {
        if (isNewSubject) {
            // First, get the next value from the sequence or find max id and increment
            int newSubjectId = getNextSubjectId(connection);
            
            // Now insert with the explicit subject_id
            String query = "INSERT INTO subjects (subject_id, subject_code, pre_requisites, description, units, year_level, semester) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
                    
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, newSubjectId);
                statement.setString(2, subject.getSubjectCode());
                statement.setString(3, subject.getPrerequisite());
                statement.setString(4, subject.getDescription());
                statement.setDouble(5, subject.getUnit());
                statement.setString(6, subject.getYearLevel());
                statement.setString(7, subject.getSemester());
                
                int rowsAffected = statement.executeUpdate();
                
                if (rowsAffected > 0) {
                    setupInitialSubjects();  // Refresh data
                    return true;
                }
                return false;
            }
        } else {
            // For updates, we need to find the subject_id first based on subject_code
            int subjectId = getSubjectIdByCode(connection, subject.getSubjectCode());
            
            if (subjectId == -1) {
                showAlert("Update Error", "Could not find subject with code: " + subject.getSubjectCode());
                return false;
            }
            
            String query = "UPDATE subjects SET pre_requisites = ?, description = ?, units = ?, " +
                    "year_level = ?, semester = ? WHERE subject_id = ?";
                    
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, subject.getPrerequisite());
                statement.setString(2, subject.getDescription());
                statement.setDouble(3, subject.getUnit());
                statement.setString(4, subject.getYearLevel());
                statement.setString(5, subject.getSemester());
                statement.setInt(6, subjectId);
                
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Database Error", 
                isNewSubject ? "Failed to add new subject: " : "Failed to update subject: " + e.getMessage());
        return false;
    }
}

// Helper method to get the next available subject_id
private int getNextSubjectId(Connection connection) throws SQLException {
    // Try to get the next value from a sequence if it exists
    try {
        String sequenceQuery = "SELECT nextval('subjects_subject_id_seq')";
        try (PreparedStatement stmt = connection.prepareStatement(sequenceQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
    } catch (SQLException e) {
        // Sequence doesn't exist, fallback to finding max and incrementing
        System.out.println("Sequence not found, getting max ID instead: " + e.getMessage());
    }
    
    // Find the maximum subject_id and increment by 1
    String maxIdQuery = "SELECT COALESCE(MAX(subject_id), 0) + 1 FROM subjects";
    try (PreparedStatement stmt = connection.prepareStatement(maxIdQuery);
         ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
            return rs.getInt(1);
        }
    }
    
    // Default to 1 if we can't determine next ID
    return 1;
}

// Helper method to get subject_id by subject_code
private int getSubjectIdByCode(Connection connection, String subjectCode) throws SQLException {
    String query = "SELECT subject_id FROM subjects WHERE subject_code = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
        stmt.setString(1, subjectCode);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("subject_id");
            }
        }
    }
    return -1; // Not found
}

// Helper method to get the next available subject_id via stored function if available
private boolean deleteSubjectFromDatabase(SubjectManagement subject) {
    try (Connection connection = DBConnection.getConnection()) {
        // First, find the subject_id based on subject_code
        int subjectId = getSubjectIdByCode(connection, subject.getSubjectCode());
        
        if (subjectId == -1) {
            showAlert("Delete Error", "Could not find subject with code: " + subject.getSubjectCode());
            return false;
        }
        
        String query = "DELETE FROM subjects WHERE subject_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, subjectId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        }
    } catch (SQLException e) {
        e.printStackTrace();
        showAlert("Database Error", "Failed to delete subject: " + e.getMessage());
        return false;
    }
}
}