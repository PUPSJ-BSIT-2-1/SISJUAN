package com.example.pupsis_main_dashboard.controllers;

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
import javafx.scene.control.TextField;
import javafx.scene.control.TableRow;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradingModuleController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(GradingModuleController.class);
    
    @FXML
    private TextField searchBar;

    @FXML
    private Label facultyName;

    @FXML
    private Label facultyID;

    @FXML
    private TableView<Subject> subjectsTable;

    @FXML
    private TableColumn<Subject, String> yearSecCol;

    @FXML
    private TableColumn<Subject, String> semCol;

    @FXML
    private TableColumn<Subject, String> subjCodeCol;

    @FXML
    private TableColumn<Subject, String> subjDescCol;

    private final ObservableList<Subject> subjectsList = FXCollections.observableArrayList();
    private final ObservableList<Subject> originalSubjectsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Verify FXML components
        validateFXMLComponents();

        // Initialize table columns
        initializeTableColumns();

        // Setup the search functionality
        setupSearch();

        // Setup row click handler
        setupRowClickHandler();

        // Load initial data
        loadSubjectsData();
    }

    private void validateFXMLComponents() {
        if (subjectsTable == null) {
            logger.error("subjectsTable is null. Check FXML file for proper fx:id.");
            throw new RuntimeException("Failed to initialize: subjectsTable is null");
        }
        
        if (facultyID == null) {
            logger.error("facultyID label is null. Check FXML file for proper fx:id.");
            throw new RuntimeException("Failed to initialize: facultyID is null");
        }
    }

    private void initializeTableColumns() {
        yearSecCol.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
        semCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        subjDescCol.setCellValueFactory(new PropertyValueFactory<>("subjectDescription"));
    }

    private void loadSubjectsData() {
        // Clear existing data
        subjectsList.clear();
        
        // Get faculty ID safely
        String currentFacultyId = facultyID.getText();
        if (currentFacultyId == null || currentFacultyId.trim().isEmpty()) {
            logger.error("Faculty ID is null or empty");
            return;
        }

        try (Connection conn = dbConnection2.getConnection()) {
            String query = "SELECT * FROM subjects WHERE faculty_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, currentFacultyId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Subject subject = new Subject(
                            rs.getString("year_section"),
                            rs.getString("semester"),
                            rs.getString("subject_code"),
                            rs.getString("subject_description")
                        );
                        subjectsList.add(subject);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while loading subjects: ", e);
            throw new RuntimeException("Failed to load subjects data", e);
        }

        // Update the original list for search functionality
        originalSubjectsList.setAll(subjectsList);
    }

    // ... rest of your existing methods (setupSearch, setupRowClickHandler, etc.) ...
}