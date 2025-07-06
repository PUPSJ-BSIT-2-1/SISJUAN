package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Subject;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FacultyClassPreviewController implements Initializable {
    @FXML private TextField searchBar; // Add this field
    @FXML private TableView<Subject> subjectsTable;
    @FXML private TableColumn<Subject, String> yearSecCol;
    @FXML private TableColumn<Subject, String> semCol;
    @FXML private TableColumn<Subject, String> subjCodeCol;
    @FXML private TableColumn<Subject, String> subjDescCol;
    @FXML private StackPane searchIconContainer;

    private final ObservableList<Subject> subjectsList = FXCollections.observableArrayList();
    // Keep a reference to the original data
    private final ObservableList<Subject> originalSubjectsList = FXCollections.observableArrayList();
    private String facultyId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize UI components first
        yearSecCol.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
        semCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        subjDescCol.setCellValueFactory(new PropertyValueFactory<>("subjectDescription"));

        yearSecCol.setReorderable(false);
        semCol.setReorderable(false);
        subjCodeCol.setReorderable(false);
        subjDescCol.setReorderable(false);

        // Set up the search icon click handler
        if (searchIconContainer != null) {
            searchIconContainer.setOnMouseClicked(event -> {
                // Your click handler code
            });
        }

        var columns = new TableColumn[]{yearSecCol, semCol, subjCodeCol, subjDescCol};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        // Show loading indicator
        subjectsTable.setPlaceholder(new Label("Loading data..."));

        // Try to get student ID with a small delay to ensure SessionData is populated
        Platform.runLater(() -> {
            facultyId = SessionData.getInstance().getFacultyId();
            if (facultyId != null && !facultyId.isEmpty()) {
                // Load data asynchronously
                Task<ObservableList<Subject>> loadTask = getObservableListTask();
                new Thread(loadTask).start();
            } else {
                subjectsTable.setPlaceholder(new Label("No faculty ID available"));
            }
        });
    }

    private String attemptToRetrieveStudentId() {
        // Try to get student ID from a label if available
        Node studentIdLabel = subjectsTable.getScene() != null ?
                subjectsTable.getScene().lookup("#studentIdLabel") : null;

        if (studentIdLabel instanceof Label) {
            String id = ((Label) studentIdLabel).getText();
            if (id != null && !id.isEmpty()) {
                SessionData.getInstance().setStudentId(id);
                return id;
            }
        }
        return null;
    }

    private Task<ObservableList<Subject>> getObservableListTask() {
        Task<ObservableList<Subject>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Subject> call() throws Exception {
                return loadSubjectsDataAsync();
            }
        };

        loadTask.setOnSucceeded(e -> {
            subjectsList.setAll(loadTask.getValue());
            originalSubjectsList.setAll(subjectsList);
            setupSearch();
            setupRowClickHandler();
        });

        loadTask.setOnFailed(e -> {
            subjectsTable.setPlaceholder(new Label("Error loading data"));
            loadTask.getException().printStackTrace();
        });
        return loadTask;
    }
    private void setupRowClickHandler() {
        subjectsTable.setRowFactory(tv -> {
            TableRow<Subject> row = new TableRow<>() {
                @Override
                protected void updateItem(Subject item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        // Remove hover styles for empty rows
                        getStyleClass().add("empty-row");
                    } else {
                        getStyleClass().remove("empty-row");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    Subject selectedSubject = row.getItem();
                    String subjectCode = selectedSubject.getSubjectCode();
                    String subjectDesc = selectedSubject.getSubjectDescription();
                    String yearSection = selectedSubject.getYearSection();

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/fxml/FacultyClassList.fxml"));
                        Parent newContent = loader.load();

                        // Get the controller after loading
                        FacultyClassListController controller = loader.getController();

                        controller.setSubjectCodeAndDesc(subjectCode, subjectDesc, yearSection);

                        Platform.runLater(this::refreshTable);

                            // Set the content
                        Stage stage = new Stage();
                        stage.initStyle(StageStyle.TRANSPARENT);
                        stage.initModality(Modality.APPLICATION_MODAL);
                        stage.setScene(new Scene(newContent, Color.TRANSPARENT));
                        controller.setClassDialogStage(stage);
                        stage.showAndWait();
                        } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return row;
        });
    }
    private ObservableList<Subject> loadSubjectsDataAsync() throws SQLException {

        if (facultyId == null || facultyId.isEmpty()) {
            throw new SQLException("Student ID not set");
        }

        ObservableList<Subject> tempList = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection()) {
            String query = """
                SELECT 
                    sec.section_name AS year_section, 
                    sem.semester_id, 
                    s.subject_code, 
                    s.description,
                    s.subject_id
                FROM faculty_load fl
                JOIN subjects s ON fl.subject_id = s.subject_id
                JOIN section sec ON fl.section_id = sec.section_id
                JOIN semesters sem ON fl.semester_id = sem.semester_id
                WHERE fl.faculty_id = ?::smallint
                ORDER BY sec.section_name, sem.semester_id, s.subject_code;
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, String.valueOf(facultyId));
                pstmt.setFetchSize(50);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        tempList.add(new Subject(
                                rs.getString("year_section"),
                                rs.getString("semester_id"),
                                rs.getString("subject_code"),
                                rs.getString("description")
                        ));
                    }
                }
            }
        }
        return tempList; // Make sure this is always returned
    }

    private void setupSearch() {
        // Create a filtered list wrapping the original list
        FilteredList<Subject> filteredData = new FilteredList<>(subjectsList, p -> true);

        // Add listener to searchBar text property
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(filteredData, newValue);
        });

        // Make the search field responsive to an ENTER key
        searchBar.setOnAction(event -> performSearch());

        // Wrap the FilteredList in a SortedList
        SortedList<Subject> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(subjectsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        subjectsTable.setItems(sortedData);

        subjectsTable.getColumns().forEach(column -> column.setReorderable(false));
    }
    
    // Method to handle search button clicks
    private void performSearch() {
        // Get the current text from the search field
        String searchText = searchBar.getText();
        
        // Request focus on the table to show we're done with the search input
        subjectsTable.requestFocus();
        
        // If there's an active filter and results are empty, show a helpful message
        if (!searchText.isEmpty() && subjectsTable.getItems().isEmpty()) {
            subjectsTable.setPlaceholder(new Label("No matches found for: " + searchText));
        }
    }
    
    // Method to apply filter logic - extracted for reuse
    private void applyFilter(FilteredList<Subject> filteredData, String newValue) {
        filteredData.setPredicate(subject -> {
            // If a search text is empty, display all subjects
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }

            // Convert search text to lower case
            String lowerCaseFilter = newValue.toLowerCase();

            // Match against all fields
            if (subject.getYearSection().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            if (subject.getSemester().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            if (subject.getSubjectCode().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            if (subject.getSubjectDescription().toLowerCase().contains(lowerCaseFilter)){
                return true;
            }
            return false; // Does not match
        });
    }

    // Update your refreshTable method to maintain the search functionality
    @FXML
    public void refreshTable() {
        subjectsList.clear();
        originalSubjectsList.clear();
        subjectsTable.setPlaceholder(new Label("Loading data...")); // Reset placeholder

        Task<ObservableList<Subject>> refreshTask = new Task<>() {
            @Override
            protected ObservableList<Subject> call() throws Exception {
                return loadSubjectsDataAsync();
            }
        };

        refreshTask.setOnSucceeded(e -> {
            ObservableList<Subject> newData = refreshTask.getValue();
            subjectsList.setAll(newData);
            originalSubjectsList.setAll(newData);
            // The table will automatically update its display
        });

        refreshTask.setOnFailed(e -> {
            subjectsTable.setPlaceholder(new Label("Error refreshing data"));
            refreshTask.getException().printStackTrace();
        });

        new Thread(refreshTask).start();
    }
}