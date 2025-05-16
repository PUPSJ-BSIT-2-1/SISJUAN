package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import com.example.pupsis_main_dashboard.databaseOperations.dbConnection2;
import com.example.pupsis_main_dashboard.utility.SessionData;
import com.example.pupsis_main_dashboard.utility.Subject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TextField;
import javafx.scene.control.TableRow;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import java.io.IOException;
import java.util.Objects;

import javafx.scene.Node;

public class GradingModuleController implements Initializable {
    @FXML private TextField searchBar; // Add this field
    @FXML private TableView<Subject> subjectsTable;
    @FXML private TableColumn<Subject, String> yearSecCol;
    @FXML private TableColumn<Subject, String> semCol;
    @FXML private TableColumn<Subject, String> subjCodeCol;
    @FXML private TableColumn<Subject, String> subjDescCol;
    @FXML private Label validationLabel;

    private final ObservableList<Subject> subjectsList = FXCollections.observableArrayList();
    // Keep a reference to the original data
    private final ObservableList<Subject> originalSubjectsList = FXCollections.observableArrayList();
    private String studentId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        studentId = SessionData.getInstance().getStudentId();

        // Add debug logging
        if (studentId == null || studentId.isEmpty()) {
            System.err.println("Warning: Student ID is not set during GradingModuleController initialization");
            // Optional: Add a retry mechanism
            studentId = attemptToRetrieveStudentId();
        }

        // Initialize UI components first
        yearSecCol.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
        semCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        subjDescCol.setCellValueFactory(new PropertyValueFactory<>("subjectDescription"));

        // Show loading indicator
        subjectsTable.setPlaceholder(new Label("Loading data..."));

        // Load data asynchronously
        Task<ObservableList<Subject>> loadTask = getObservableListTask();
        new Thread(loadTask).start();
    }

    private String attemptToRetrieveStudentId() {
        // Try to get student ID from label if available
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
            TableRow<Subject> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    Subject selectedSubject = row.getItem();
                    try {
                        ScrollPane contentPane = (ScrollPane) subjectsTable.getScene().lookup("#contentPane");
                        if (contentPane != null) {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                                "/com/example/pupsis_main_dashboard/fxml/newEditingGradePage.fxml"));
                            Parent newContent = loader.load();
                        
                        // Get the controller to pass data if needed
                        // NewEditingGradePageController controller = loader.getController();
                        // controller.initData(selectedSubject);
                        
                            contentPane.setContent(newContent);
                        } else {
                            System.err.println("Error: Could not find contentPane");
                        }
                    } catch (IOException e) {
                        System.err.println("Error loading newEditingGradePage.fxml: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            return row;
        });
    }
    private ObservableList<Subject> loadSubjectsDataAsync() throws SQLException {

        validationLabel.setText(studentId);
        if (studentId == null || studentId.isEmpty()) {
            throw new SQLException("Student ID not set");
        }

        ObservableList<Subject> tempList = FXCollections.observableArrayList();
        try (Connection conn = dbConnection2.getConnection()) {
            String query = "SELECT year_section, semester, subject_code, subject_description " +
                    "FROM subjects WHERE student_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, studentId);
                pstmt.setFetchSize(50);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        tempList.add(new Subject(
                                rs.getString("year_section"),
                                rs.getString("semester"),
                                rs.getString("subject_code"),
                                rs.getString("subject_description")
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
            filteredData.setPredicate(subject -> {
                // If search text is empty, display all subjects
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
                return subject.getSubjectDescription().toLowerCase().contains(lowerCaseFilter);// Does not match
            });
        });

        // Wrap the FilteredList in a SortedList
        SortedList<Subject> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(subjectsTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        subjectsTable.setItems(sortedData);

        subjectsTable.getColumns().forEach(column -> column.setReorderable(false));
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