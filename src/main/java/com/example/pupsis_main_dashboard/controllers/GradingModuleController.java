package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utility.Student;
import com.example.pupsis_main_dashboard.utility.Subject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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


public class GradingModuleController implements Initializable {
    @FXML
    private TextField searchBar; // Add this field

    @FXML
    private Label facultyName;

    @FXML
    private Label facultyID;

    @FXML
    private TableView<Subject> subjectsTable;

    @FXML
    private TableColumn<Subject, String> editBtnCol;

    @FXML
    private TableColumn<Subject, String> yearSecCol;

    @FXML
    private TableColumn<Subject, String> semCol;

    @FXML
    private TableColumn<Subject, String> subjCodeCol;

    @FXML
    private TableColumn<Subject, String> subjDescCol;

    private final ObservableList<Subject> subjectsList = FXCollections.observableArrayList();

    // Keep a reference to the original data
    private final ObservableList<Subject> originalSubjectsList = FXCollections.observableArrayList();

    // Database connection constants
    private static final String URL = "jdbc:postgresql://db.autqwzshfjaqbkxpiqxm.supabase.co:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "pupSISProject2025";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Verify that FXML injection worked
        if (subjectsTable == null) {
            System.err.println("Error: subjectsTable is null. Check FXML file for proper fx:id.");
            return;
        }

        // Initialize the columns
        yearSecCol.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
        semCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        subjDescCol.setCellValueFactory(new PropertyValueFactory<>("subjectDescription"));

        // Load data from database (only call once)
        loadSubjectsData();

        // Store in original list
        originalSubjectsList.addAll(subjectsList);

        // Setup the search functionality
        setupSearch();

        // Add this line to setup the row click handler
        setupRowClickHandler();
    }

    private void setupRowClickHandler() {
        subjectsTable.setRowFactory(tv -> {
            TableRow<Subject> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    OpenNewGradingModule newModule = new OpenNewGradingModule(subjectsTable);
                    newModule.open();
                }
            });
            return row;
        });
    }
    private void loadSubjectsData() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String query = "SELECT * FROM subjects WHERE faculty_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, facultyID.getText());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        subjectsList.add(new Subject(
                                rs.getString("year_section"),
                                rs.getString("semester"),
                                rs.getString("subject_code"),
                                rs.getString("subject_description")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
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
                if (subject.getSubjectDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false; // Does not match
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
        loadSubjectsData();
        originalSubjectsList.addAll(subjectsList);
    }
}