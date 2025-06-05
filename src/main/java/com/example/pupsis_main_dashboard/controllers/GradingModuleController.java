package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester; 
import com.example.pupsis_main_dashboard.utilities.SessionData;
import com.example.pupsis_main_dashboard.models.Subject;
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
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradingModuleController implements Initializable {
    @FXML private TextField searchBar; 
    @FXML private TableView<Subject> subjectsTable;
    @FXML private TableColumn<Subject, String> yearSecCol;
    @FXML private TableColumn<Subject, String> semCol;
    @FXML private TableColumn<Subject, String> subjCodeCol;
    @FXML private TableColumn<Subject, String> subjDescCol;
    @FXML private Label validationLabel; 
    @FXML private StackPane searchIconContainer;

    private final ObservableList<Subject> subjectsList = FXCollections.observableArrayList();
    private final ObservableList<Subject> originalSubjectsList = FXCollections.observableArrayList();
    private String facultyNumber; 
    private int integerFacultyId; 

    private static final Logger logger = LoggerFactory.getLogger(GradingModuleController.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("GradingModuleController initializing...");
        yearSecCol.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
        semCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        subjCodeCol.setCellValueFactory(new PropertyValueFactory<>("subjectCode"));
        subjDescCol.setCellValueFactory(new PropertyValueFactory<>("subjectDescription"));

        yearSecCol.setReorderable(false);
        semCol.setReorderable(false);
        subjCodeCol.setReorderable(false);
        subjDescCol.setReorderable(false);

        if (searchIconContainer != null) {
            searchIconContainer.setOnMouseClicked(event -> {
                logger.debug("Search icon clicked.");
                // Implement search logic or ensure searchBar handles it
            });
        }

        subjectsTable.setPlaceholder(new Label("Loading subjects..."));

        Platform.runLater(() -> {
            this.facultyNumber = SessionData.getInstance().getFacultyId(); 
            logger.info("Retrieved faculty_number from SessionData: '{}'", this.facultyNumber);

            if (this.facultyNumber != null && !this.facultyNumber.isEmpty()) {
                this.integerFacultyId = getIntegerFacultyIdByFacultyNumber(this.facultyNumber);
                if (this.integerFacultyId != 0) { 
                    logger.info("Successfully obtained integer faculty_id: {}", this.integerFacultyId);
                    validationLabel.setText("Faculty ID: " + this.facultyNumber + " (PK: " + this.integerFacultyId + ")"); 
                    Task<ObservableList<Subject>> loadTask = getObservableListTask();
                    new Thread(loadTask).start();
                } else {
                    logger.error("Failed to obtain integer faculty_id for faculty_number: {}. Cannot load subjects.", this.facultyNumber);
                    subjectsTable.setPlaceholder(new Label("Error: Invalid faculty identifier."));
                    validationLabel.setText("Error: Invalid Faculty ID");
                }
            } else {
                logger.warn("Faculty_number is null or empty from SessionData. Cannot load subjects.");
                subjectsTable.setPlaceholder(new Label("No faculty identifier available."));
                validationLabel.setText("Faculty ID not available");
            }
        });
    }

    private Task<ObservableList<Subject>> getObservableListTask() {
        Task<ObservableList<Subject>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Subject> call() throws Exception {
                logger.debug("Background task started for loading subjects data.");
                return loadSubjectsDataAsync();
            }
        };

        loadTask.setOnSucceeded(e -> {
            subjectsList.setAll(loadTask.getValue());
            originalSubjectsList.setAll(subjectsList);
            if (subjectsList.isEmpty()){
                 subjectsTable.setPlaceholder(new Label("No subjects assigned or found for the current semester."));
                 logger.info("No subjects loaded for faculty_id: {}", integerFacultyId);
            } else {
                 logger.info("Successfully loaded {} subjects for faculty_id: {}", subjectsList.size(), integerFacultyId);
            }
            setupSearch();
            setupRowClickHandler();
        });

        loadTask.setOnFailed(e -> {
            Throwable exception = loadTask.getException();
            logger.error("Failed to load subjects data for faculty_id: {}. Error: {}", integerFacultyId, exception.getMessage(), exception);
            subjectsTable.setPlaceholder(new Label("Error loading subjects. Please check logs."));
        });
        return loadTask;
    }

    private void setupRowClickHandler() {
        subjectsTable.setRowFactory(tv -> {
            TableRow<Subject> row = new TableRow<>() {
                @Override
                protected void updateItem(Subject item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { 
                        getStyleClass().remove("filled-row"); 
                        getStyleClass().add("empty-row");
                    } else {
                        getStyleClass().remove("empty-row");
                        getStyleClass().add("filled-row"); 
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    Subject selectedSubject = row.getItem();
                    if (selectedSubject == null) return; 

                    String subjectCode = selectedSubject.getSubjectCode();
                    String subjectDesc = selectedSubject.getSubjectDescription();
                    logger.info("Double-clicked on subject: Code='{}', Description='{}'", subjectCode, subjectDesc);

                    try {
                        ScrollPane contentPane = (ScrollPane) subjectsTable.getScene().lookup("#contentPane");
                        if (contentPane != null) {
                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/com/example/pupsis_main_dashboard/fxml/EditGradesPage.fxml")
                            );
                            Parent newContent = loader.load();
                            EditGradesPageController controller = loader.getController();
                            controller.setSubjectCode(selectedSubject.getSubjectCode()); 
                            controller.setSubjectDesc(selectedSubject.getSubjectDescription()); 
                            logger.debug("Navigating to EditGradesPage for subject code: {}", subjectCode);
                            contentPane.setContent(newContent);
                        } else {
                            logger.warn("contentPane lookup returned null. Cannot navigate.");
                        }
                    } catch (IOException e) { 
                        logger.error("Error loading EditGradesPage.fxml or navigating: {}", e.getMessage(), e);
                    }
                }
            });
            return row;
        });
    }

    private ObservableList<Subject> loadSubjectsDataAsync() throws SQLException {
        if (this.integerFacultyId == 0) { 
            logger.error("loadSubjectsDataAsync: integerFacultyId is 0. Cannot load subjects.");
            throw new SQLException("Faculty ID (integer) not set or invalid.");
        }

        ObservableList<Subject> tempList = FXCollections.observableArrayList();
        logger.debug("loadSubjectsDataAsync: Loading all subjects for faculty_id: {}", this.integerFacultyId);

        String query = """
            SELECT ys.year_section, s.subject_code, s.description
            FROM faculty_load fl
            JOIN subjects s ON fl.subject_id = s.subject_id
            JOIN year_section ys ON fl.section_id = ys.section_id
            WHERE fl.faculty_id = ?;
            """;
        
        logger.debug("Executing query: {}", query);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, this.integerFacultyId);

            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    tempList.add(new Subject(
                            rs.getString("year_section"),
                            "",
                            rs.getString("subject_code"),
                            rs.getString("description")
                    ));
                }
                logger.info("loadSubjectsDataAsync: Loaded {} subjects from database.", count);
            }
        } catch (SQLException e) {
            logger.error("loadSubjectsDataAsync: SQL error while loading subjects for faculty_id: {}. Error: {}", this.integerFacultyId, e.getMessage(), e);
            throw e; 
        }
        return tempList;
    }

    private void setupSearch() {
        if (searchBar == null) {
            logger.warn("Search bar is null. Search functionality will not be available.");
            return;
        }
        FilteredList<Subject> filteredData = new FilteredList<>(originalSubjectsList, p -> true);

        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(subject -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (subject.getSubjectCode().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (subject.getSubjectDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (subject.getYearSection().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else return subject.getSemester().toLowerCase().contains(lowerCaseFilter);
            });
            logger.trace("Search filter applied with text: '{}'. Filtered count: {}", newValue, filteredData.size());
        });

        SortedList<Subject> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(subjectsTable.comparatorProperty());
        subjectsTable.setItems(sortedData);
        if (originalSubjectsList.isEmpty()){
             subjectsTable.setPlaceholder(new Label("No subjects assigned or found for the current semester."));
        }
        logger.debug("Search functionality set up.");
    }

    private int getIntegerFacultyIdByFacultyNumber(String facultyNumber) {
        if (facultyNumber == null || facultyNumber.trim().isEmpty()) {
            logger.warn("getIntegerFacultyIdByFacultyNumber: facultyNumber is null or empty.");
            return 0;
        }
        String sql = "SELECT faculty_id FROM faculty WHERE faculty_number = ?";
        logger.debug("Attempting to fetch integer faculty_id for faculty_number: {}", facultyNumber);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, facultyNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("faculty_id");
                    logger.info("Successfully fetched integer faculty_id: {} for faculty_number: {}", id, facultyNumber);
                    return id;
                } else {
                    logger.warn("No faculty record found for faculty_number: {}", facultyNumber);
                    return 0; 
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error while fetching faculty_id for faculty_number: {}. Error: {}", facultyNumber, e.getMessage(), e);
            return 0; 
        }
    }
}