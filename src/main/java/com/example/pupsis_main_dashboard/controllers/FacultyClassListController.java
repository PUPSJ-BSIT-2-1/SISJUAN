package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Student;
import com.example.pupsis_main_dashboard.models.Subject;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import com.example.pupsis_main_dashboard.utilities.StudentCache;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FacultyClassListController {

    @FXML private TableView<Student> classListTable;
    @FXML private TableColumn<Student, String> rowCountColumn;
    @FXML private TableColumn<Student, String> studentNumberColumn;
    @FXML private TableColumn<Student, String> studentNameColumn;
    @FXML private TableColumn<Student, String> emailColumn;
    @FXML private TableColumn<Student, String> birthdayColumn;
    @FXML private TableColumn<Student, String> addressColumn;
    @FXML private TableColumn<Student, String> statusColumn;
    @FXML private Label headerLabel;
    @FXML private Label subjectDescriptionLabel;
    @FXML private TextField searchBar;

    private String selectedSubjectCode;
    private String selectedSubjectDesc;
    private String selectedYearSection;

    private Stage dialogStage;

    private static final Logger logger = LoggerFactory.getLogger(FacultyClassListController.class.getName());
    private final ObservableList<Student> classList = FXCollections.observableArrayList();
    private final StudentCache studentCache = StudentCache.getInstance();

    @FXML private void initialize() {

        logger.info("FacultyClassListController initializing...");

        classListTable.getColumns().forEach(column -> column.setReorderable(false));
        classListTable.getColumns().forEach(column -> column.setSortable(false));

        rowCountColumn.setCellValueFactory(new PropertyValueFactory<>("studentNo"));
        studentNumberColumn.setCellValueFactory(new PropertyValueFactory<>("StudentNumber"));
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        birthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public void setClassDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setSubjectCodeAndDesc(String subjectCode, String subjectDesc, String yearSection) {
        this.selectedSubjectCode = subjectCode;
        this.selectedSubjectDesc = subjectDesc;
        this.selectedYearSection = yearSection;
        loadStudentsBySubjectCode(subjectCode, yearSection);
        headerLabel.setText("Class List for " + "(" + yearSection + ")");
        subjectDescriptionLabel.setText("Subject Description: " + subjectDesc);
    }

    private void loadStudentsBySubjectCode(String subjectCode, String yearSection) {
        logger.debug("Loading students for Subject Code and Year & Section: {}", subjectCode);
        ObservableList<Student> tempClassList = FXCollections.observableArrayList();
        Task<ObservableList<Student>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Student> call() throws Exception {
                int rowNum = 1; // Initialize counter for auto-incrementing numbers

                try (Connection conn = DBConnection.getConnection()) {
                    String query = """
                        SELECT
                        s.student_number,
                        s.firstname,
                        s.lastname,
                        s.middlename,
                        s.birthday,
                        s.email,
                        s.address,
                        ss.status_name,
                        fl.load_id
                        FROM student_load sl
                        JOIN students s ON sl.student_pk_id = s.student_id
                        JOIN student_statuses ss ON ss.student_status_id = s.student_status_id
                        JOIN faculty_load fl ON fl.load_id = sl.faculty_load
                        JOIN subjects subj ON fl.subject_id = subj.subject_id
                        JOIN section sec ON fl.section_id = sec.section_id
                        WHERE subj.subject_code = ?
                        AND sec.section_name = ?;
                    """;
                    try (PreparedStatement pstmt = conn.prepareStatement(query)) {

                        pstmt.setFetchSize(70);
                        pstmt.setString(1, subjectCode);
                        pstmt.setString(2, yearSection);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next() && !isCancelled()) {
                                Student student = new Student(
                                        String.valueOf(rowNum++),
                                        rs.getString("student_number"),
                                        String.format("%s, %s %s",
                                                rs.getString("lastname"),
                                                rs.getString("firstname"),
                                                rs.getString("middlename") == null ? "" : rs.getString("middlename")),
                                        rs.getString("email"),
                                        rs.getString("address") == null ? "" : rs.getString("address"),
                                        rs.getString("status_name"),
                                        rs.getString("birthday") == null ? "" : rs.getString("birthday").substring(0, 10)
                                );
                                tempClassList.add(student);
                            }
                        }
                    }
                    return tempClassList;
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            ObservableList<Student> result = loadTask.getValue();
            studentCache.put(subjectCode, result);
            updateTableView(result);
            setupSearch();
        });

        loadTask.setOnFailed(e -> {
            Throwable ex = loadTask.getException();
            logger.error("Error loading students by subject code: {}", subjectCode, ex);
            StageAndSceneUtils.showAlert("Error", "Failed to load students", Alert.AlertType.ERROR);
        });

        new Thread(loadTask).start();
    }

    private void updateTableView(ObservableList<Student> students) {
        Platform.runLater(() -> {
            classList.clear();
            classList.addAll(students);
        });
    }

    private void applyFilter(FilteredList<Student> filteredData, String newValue) {
        filteredData.setPredicate(student -> {
            if (newValue == null || newValue.isEmpty()) return true;
            String lowerCaseFilter = newValue.toLowerCase();
            if (student.getStudentId().toLowerCase().contains(lowerCaseFilter)) return true;
            if (student.getFullNa().toLowerCase().contains(lowerCaseFilter)) return true;
            if (student.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
            if (student.getAddress().toLowerCase().contains(lowerCaseFilter)) return true;
            if (student.getStatus().toLowerCase().contains(lowerCaseFilter)) return true;
            if (student.getBirthday().toLowerCase().contains(lowerCaseFilter)) return true;
            return false;
        });
    }

    private void updateRowNumbers(FilteredList<Student> filteredData) {
        int rowNum = 1;
        for (Student stud : filteredData) {
            stud.setStudentNo(String.valueOf(rowNum++));
        }
    }

    private void updatePlaceholder(FilteredList<Student> filteredData, String searchText) {
        if (!searchText.isEmpty() && filteredData.isEmpty()) {
            classListTable.setPlaceholder(new Label("No matches found for: " + searchText));
        } else {
            classListTable.setPlaceholder(new Label("No students to display"));
        }
    }

    private void setupSearch() {
        // Create a filtered list wrapping the original list
        FilteredList<Student> filteredData = new FilteredList<>(classList, p -> true);

        // Add listener to searchBar text property
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(filteredData, newValue);
            updateRowNumbers(filteredData);
            updatePlaceholder(filteredData, newValue);
        });

        // Make the search field responsive to an ENTER key
        searchBar.setOnAction(event -> {
            updatePlaceholder(filteredData, searchBar.getText());
        });

        // Wrap the FilteredList in a SortedList
        SortedList<Student> sortedData = new SortedList<>(filteredData);

        // Bind the SortedList comparator to the TableView comparator
        sortedData.comparatorProperty().bind(classListTable.comparatorProperty());

        // Add sorted (and filtered) data to the table
        classListTable.setItems(sortedData);

        classListTable.getColumns().forEach(column -> column.setReorderable(false));
    }

}