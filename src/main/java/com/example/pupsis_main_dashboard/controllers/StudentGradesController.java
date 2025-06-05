package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Grades;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class StudentGradesController {
    @FXML
    private VBox root;
    @FXML
    private TableView<Grades> studentsTable;
    @FXML
    private TableColumn<Grades, String> subCode;
    @FXML
    private TableColumn<Grades, String> subDescription;
    @FXML
    private TableColumn<Grades, String> facultyName;
    @FXML
    private TableColumn<Grades, String> units;
    @FXML
    private TableColumn<Grades, String> sectionCode;
    @FXML
    private TableColumn<Grades, String> finGrade;
    @FXML
    private TableColumn<Grades, String> gradeStatus;
    @FXML
    private ComboBox<String> semesterComboBox;
    @FXML
    private ComboBox<String> yearSectionComboBox;
    @FXML
    private Label yearLevel;
    @FXML
    private Label scholasticStatus;
    @FXML
    private Label semesterGPA;

    private static double borrowGPA = 0;
    private final ObservableList<Grades> studentsList = FXCollections.observableArrayList();
    private final Logger logger = LoggerFactory.getLogger(StudentGradesController.class);

    public void initialize() {
        studentsTable.setEditable(false);
        populateYearSection();
        populateSemester();
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                loadGrades();
                return null;
            }
        };
        new Thread(loadTask).start();

        subCode.setCellValueFactory(new PropertyValueFactory<>("subCode"));
        subDescription.setCellValueFactory(new PropertyValueFactory<>("subDesc"));
        facultyName.setCellValueFactory(new PropertyValueFactory<>("facultyName"));
        units.setCellValueFactory(new PropertyValueFactory<>("units"));
        sectionCode.setCellValueFactory(new PropertyValueFactory<>("sectionCode"));
        finGrade.setCellValueFactory(new PropertyValueFactory<>("finalGrade"));
        gradeStatus.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        studentsTable.setItems(studentsList);

        TableColumn<?, ?>[] columns = new TableColumn[]{subCode, subDescription, facultyName, units, sectionCode, finGrade, gradeStatus};
        for (TableColumn<?, ?> col : columns) {
            col.setReorderable(false);
        }

        if (studentsList.isEmpty()) {
            studentsTable.setPlaceholder(new Label("No Grades available."));
        } else {
            studentsTable.setPlaceholder(new Label("Loading data..."));
        }

        studentsTable.setRowFactory(_ -> {
            TableRow<Grades> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });

        for (TableColumn<Grades, String> col : Arrays.asList(subCode, subDescription, facultyName, units, sectionCode, finGrade, gradeStatus)) {
            setWrappingHeaderCellFactory(col);
        }

        semesterComboBox.setOnAction(_ -> filterGrades());
        yearSectionComboBox.setOnAction(_ -> filterGrades());
    }

    private void setWrappingHeaderCellFactory(TableColumn<Grades, String> column) {

        AtomicBoolean isDarkTheme = new AtomicBoolean(root.getScene() != null && root.getScene().getRoot().getStyleClass().contains("dark-theme"));
        root.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                isDarkTheme.set(newScene.getRoot().getStyleClass().contains("dark-theme"));
                newScene.getRoot().getStyleClass().addListener((ListChangeListener<String>) change ->
                        isDarkTheme.set(change.getList().contains("dark-theme")));
            }
        });

        column.setCellFactory(_ -> new TableCell<>() {
            private final Label label = new Label();

            {
                String textColor = isDarkTheme.get() ? "#e0e0e0" : "#000000";
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-alignment: center; -fx-text-alignment: center; -fx-text-fill: " + textColor + ";");

                StackPane pane = new StackPane(label);
                pane.setPrefHeight(Control.USE_COMPUTED_SIZE);
                pane.setAlignment(Pos.CENTER);
                setGraphic(pane);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });
    }

    private void loadGrades() {
        String sessionStudentID = SessionData.getInstance().getStudentNumber();
        String query = """
            SELECT DISTINCT ON (s.student_id, sub.subject_id)
            s.student_id,
            s.student_number,
            s.current_year_section_id,
            scs.status_name AS scholastic_status_name,
            sub.subject_id,
            sub.subject_code,
            sub.description AS subject_description,
            sub.units,
            fac.faculty_id,
            fac.faculty_number,
            fac.firstname || ' ' || fac.lastname AS faculty_name,
            g.grade_id,
            g.final_grade,
            gs.status_name AS grade_status_name
            FROM student_load sl
            JOIN students s ON sl.student_pk_id = s.student_id
            JOIN faculty_load fl ON sl.faculty_load = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN subjects sub ON fl.subject_id = sub.subject_id
            JOIN schedule sch ON sch.faculty_load_id = fl.load_id
            JOIN room r ON sch.room_id = r.room_id
            LEFT JOIN grade g ON g.faculty_load = fl.load_id AND g.student_pk_id = s.student_id
            LEFT JOIN grade_statuses gs ON g.grade_status_id = gs.grade_status_id
            LEFT JOIN scholastic_statuses scs ON s.scholastic_status_id = scs.scholastic_status_id
            WHERE s.student_id = ? AND s.current_year_section_id = ?
            ORDER BY s.student_id, sub.subject_id, g.grade_id DESC;
            """;

        String query2 = "SELECT student_id, current_year_section_id FROM students WHERE student_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             PreparedStatement stmt2 = conn.prepareStatement(query2)) {

            if (sessionStudentID == null || sessionStudentID.isEmpty()) {
                logger.error("Session student number is null or empty");
                return;
            }
            int studentID = 0;
            int studentYearSectionId = 0; 
            stmt2.setString(1, sessionStudentID);
            try (ResultSet rs = stmt2.executeQuery()) {
                if (rs.next()) { 
                    studentID = rs.getInt("student_id");
                    studentYearSectionId = rs.getInt("current_year_section_id"); 
                } else {
                    logger.warn("No student found with student_number: {}", sessionStudentID);
                    return;
                }
            }
            stmt.setInt(1, studentID);
            stmt.setObject(2, studentYearSectionId, Types.INTEGER); 
            try (ResultSet rs = stmt.executeQuery()) {
                studentsList.clear(); 
                while (rs.next()) {
                    String scholasticStatus = rs.getString("scholastic_status_name");
                    String subCode = rs.getString("subject_code");
                    String subDescription = rs.getString("subject_description");
                    String facultyName = rs.getString("faculty_name");
                    String units = rs.getString("units");
                    String sectionCode = rs.getString("current_year_section_id"); 
                    String finGrade = rs.getString("final_grade");
                    String gradeStatus = rs.getString("grade_status_name");

                    Grades grades = new Grades(
                        scholasticStatus,
                        subCode,
                        subDescription,
                        facultyName,
                        units,
                        sectionCode,
                        finGrade,
                        gradeStatus
                    );

                    studentsList.add(grades);
                    determineScholasticStatus(grades.getScholasticStatus());
                    determineYearLevel(grades.getSectionCode());
                }
                computeSemesterGPA(); 
                if (studentsList.isEmpty()) {
                    studentsTable.setPlaceholder(new Label("No grades available for the current configuration."));
                } else {
                    studentsTable.setItems(studentsList);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading grades", ex);
            studentsTable.setPlaceholder(new Label("Error loading grades."));
        }
    }

    private void populateSemester() {
        ObservableList<String> semesters = FXCollections.observableArrayList(
            "1st Semester", "2nd Semester", "Summer Term"
        );
        semesterComboBox.setItems(semesters);
    }

    private void populateYearSection() {
        ObservableList<String> yearSections = FXCollections.observableArrayList(
                "1st Year", "2nd Year", "3rd Year", "4th Year"
        );
        yearSectionComboBox.setItems(yearSections);
    }

    private void determineYearLevel(String yearSection) {
        String[] splitYearLevel = yearSection.split("-");
        switch (splitYearLevel[0]) {
            case "1":
                yearLevel.setText("1st Year");
                break;
            case "2":
                yearLevel.setText("2nd Year");
                break;
            case "3":
                yearLevel.setText("3rd Year");
                break;
            case "4":
                yearLevel.setText("4th Year");
                break;
        }
    }

    private void determineScholasticStatus(String schStatus) {
        switch (schStatus) {
            case "Regular":
                scholasticStatus.setText("Regular");
                break;
            case "Irregular":
                scholasticStatus.setText("Irregular");
                break;
            default:
                scholasticStatus.setText("N/A");
                break;
        }
    }

    public void computeSemesterGPA() {
        double totalUnits = 0;
        double totalGradePoints = 0;
        boolean hasIncompleteGrades = false;

        for (Grades grades : studentsList) {
            String finalGrade = grades.getFinalGrade();
            if (finalGrade == null || finalGrade.isEmpty()) {
                hasIncompleteGrades = true;
                break;
            }
            double gradePoints = Double.parseDouble(finalGrade);
            int units = Integer.parseInt(grades.getUnits());
            totalUnits += units;
            totalGradePoints += gradePoints * units;
        }

        if (hasIncompleteGrades) {
            semesterGPA.setText("N/A");
        } else {
            double semGPA = totalGradePoints / totalUnits;
            semesterGPA.setText(String.format("%.2f", semGPA));
            setGWA(semGPA);
        }
    }

    private void setGWA(double semGPA) {
        borrowGPA = semGPA;
    }

    public static double getGWA() {
        return borrowGPA;
    }

    private void filterGrades() {
        String selectedSemester = semesterComboBox.getValue();
        String selectedYearSectionString = yearSectionComboBox.getValue();
        ObservableList<Grades> filteredGrades = FXCollections.observableArrayList();

        String searchGrades = """
            SELECT DISTINCT ON (s.student_id, sub.subject_id)
            s.student_id,
            s.student_number,
            s.current_year_section_id,
            scs.status_name AS scholastic_status_name,
            sub.subject_id,
            sub.subject_code,
            sub.description AS subject_description,
            sub.units,
            fac.faculty_id,
            fac.faculty_number,
            fac.firstname || ' ' || fac.lastname AS faculty_name,
            g.grade_id,
            g.final_grade,
            gs.status_name AS grade_status_name
            FROM student_load sl
            JOIN students s ON sl.student_pk_id = s.student_id
            JOIN faculty_load fl ON sl.faculty_load = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN subjects sub ON fl.subject_id = sub.subject_id
            JOIN schedule sch ON sch.faculty_load_id = fl.load_id
            JOIN room r ON sch.room_id = r.room_id
            LEFT JOIN grade g ON g.faculty_load = fl.load_id AND g.student_pk_id = s.student_id
            LEFT JOIN grade_statuses gs ON g.grade_status_id = gs.grade_status_id
            LEFT JOIN scholastic_statuses scs ON s.scholastic_status_id = scs.scholastic_status_id
            JOIN semesters sem ON sl.semester_id = sem.semester_id
            WHERE s.student_id = ? AND s.current_year_section_id = ? AND sem.semester_name = ?
            ORDER BY s.student_id, sub.subject_id, g.grade_id DESC;
           """;

        String searchStudentID = """
            SELECT student_id
            FROM students
            WHERE student_number = ?
            LIMIT 1
            """;

        String sessionStudentNumber = SessionData.getInstance().getStudentNumber();
        if (sessionStudentNumber == null || sessionStudentNumber.isEmpty()) {
            logger.error("Session student number is null or empty for filtering grades.");
            studentsTable.setPlaceholder(new Label("User session error."));
            return;
        }
        if (selectedSemester == null || selectedYearSectionString == null) {
            logger.info("Semester or Year Section not selected for filtering.");
            loadGrades(); 
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt1_searchStudentId = conn.prepareStatement(searchStudentID);
             PreparedStatement stmt3_searchGrades = conn.prepareStatement(searchGrades)) {

            stmt1_searchStudentId.setString(1, sessionStudentNumber);
            int studentID = 0;
            try (ResultSet rs = stmt1_searchStudentId.executeQuery()) {
                if (rs.next()) {
                    studentID = rs.getInt("student_id");
                } else {
                    logger.warn("No student found with student_number: {} for filtering.", sessionStudentNumber);
                    studentsTable.setPlaceholder(new Label("Student not found."));
                    return;
                }
            }

            int selectedYearSectionId;
            try {
                selectedYearSectionId = Integer.parseInt(selectedYearSectionString);
            } catch (NumberFormatException e) {
                logger.error("Invalid year section format in ComboBox: {}", selectedYearSectionString, e);
                studentsTable.setPlaceholder(new Label("Invalid year/section filter value."));
                return;
            }

            stmt3_searchGrades.setInt(1, studentID);
            stmt3_searchGrades.setObject(2, selectedYearSectionId, Types.INTEGER); 
            stmt3_searchGrades.setString(3, selectedSemester);    

            boolean hasResults = false;

            try (ResultSet rs = stmt3_searchGrades.executeQuery()) {
                while (rs.next()) {
                    hasResults = true;

                    String scholasticStatus = rs.getString("scholastic_status_name");
                    String subCode = rs.getString("subject_code");
                    String subDescription = rs.getString("subject_description");
                    String facultyName = rs.getString("faculty_name");
                    String units = rs.getString("units");
                    String sectionCode = rs.getString("current_year_section_id"); 
                    String finGrade = rs.getString("final_grade");
                    String gradeStatus = rs.getString("grade_status_name");

                    Grades grade = new Grades(scholasticStatus, subCode, subDescription, facultyName, units, sectionCode, finGrade, gradeStatus);
                    filteredGrades.add(grade);
                }
            }
            if (!hasResults) {
                studentsTable.setItems(FXCollections.observableArrayList()); 
                studentsTable.setPlaceholder(new Label("No grades available for the selected filters."));
            } else {
                studentsTable.setItems(filteredGrades);
                if (!filteredGrades.isEmpty()) {
                     Grades firstGrade = filteredGrades.get(0);
                     determineScholasticStatus(firstGrade.getScholasticStatus());
                     determineYearLevel(firstGrade.getSectionCode());
                }
                computeSemesterGPA(); 
            }
        } catch (SQLException e) {
            logger.error("Failed to filter grades", e);
            studentsTable.setPlaceholder(new Label("Error filtering grades."));
        }
    }
}
