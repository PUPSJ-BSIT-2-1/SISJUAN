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

public class SubjectController implements Initializable {

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
        refreshButton.setOnAction(e -> updateFilter());
    }

    private void setupInitialSubjects() {
        // Clear existing subjects if any
        allSubjects.clear();

        // Fetch subjects from Supabase
        try (Connection connection = DBConnection.getConnection()) {
            String query = "SELECT subject_code, pre_requisites, description, units, year_level, semester " +
                    "FROM subjects ORDER BY year_level, semester, subject_code";

            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String subjectCode = resultSet.getString("subject_code");
                    String preRequisites = resultSet.getString("pre_requisites");
                    String description = resultSet.getString("description");
                    double units = resultSet.getDouble("units");
                    String yearLevel = resultSet.getString("year_level");
                    String semester = resultSet.getString("semester");

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
            addSubject(newSubject);
        }
    }

    private void handleEdit() {
        SubjectManagement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            SubjectManagement updatedSubject = showSubjectForm(selected);
            if (updatedSubject != null) {
                tableView.refresh();
            }
        } else {
            showAlert("Edit Failed", "Please select a subject to edit.");
        }
    }

    private void handleDelete() {
        SubjectManagement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            allSubjects.remove(selected);
            updateFilter();
        } else {
            showAlert("Delete Failed", "Please select a subject to delete.");
        }
    }

    private SubjectManagement showSubjectForm(SubjectManagement subject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/subjectmodule/fxml/SubjectForm.fxml"));
            Parent parent = loader.load();

            SubjectFormController controller = loader.getController();
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
            showAlert("Form Load Error", "Unable to open the subject form.");
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
        String query;

        if (isNewSubject) {
            query = "INSERT INTO subjects (subject_code, pre_requisites, description, units, year_level, semester) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        } else {
            query = "UPDATE subjects SET pre_requisites = ?, description = ?, units = ?, " +
                    "year_level = ?, semester = ? WHERE subject_code = ?";
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            if (isNewSubject) {
                statement.setString(1, subject.getSubjectCode());
                statement.setString(2, subject.getPreRequisites());
                statement.setString(3, subject.getDescription());
                statement.setDouble(4, subject.getUnits());
                statement.setString(5, subject.getYearLevel());
                statement.setString(6, subject.getSemester());
            } else {
                statement.setString(1, subject.getPreRequisites());
                statement.setString(2, subject.getDescription());
                statement.setDouble(3, subject.getUnits());
                statement.setString(4, subject.getYearLevel());
                statement.setString(5, subject.getSemester());
                statement.setString(6, subject.getSubjectCode());
            }

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error",
                    isNewSubject ? "Failed to add new subject: " : "Failed to update subject: " + e.getMessage());
            return false;
        }
    }
    private boolean deleteSubjectFromDatabase(SubjectManagement subject) {
        String query = "DELETE FROM subjects WHERE subject_code = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, subject.getSubjectCode());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to delete subject: " + e.getMessage());
            return false;
        }
    }
}