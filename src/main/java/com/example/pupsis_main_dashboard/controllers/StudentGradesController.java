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
        System.out.println("STUDENT_ID:" + sessionStudentID);
        String query = """
            SELECT DISTINCT ON (s.student_id, sub.subject_id)
            s.student_id,
            s.student_number,
            s.year_section,
            s.scholastic_status,
            sub.subject_id,
            sub.subject_code,
            sub.description,
            sub.units,
            fac.faculty_id,
            fac.faculty_number,
            fac.firstname || ' ' || fac.lastname AS faculty_name,
            g.grade_id,
            g.final_grade,
            g.gradestat
            FROM student_load sl
            JOIN students s ON sl.student_id = s.student_id
            JOIN faculty_load fl ON sl.load_id = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN subjects sub ON fl.subject_id = sub.subject_id
            JOIN schedule sch ON fl.load_id = sl.load_id
            JOIN room r ON sch.room_id = r.room_id
            LEFT JOIN grade g ON g.faculty_load = fl.load_id AND g.student_id = s.student_number
            WHERE s.student_id = ? AND fl.year_section = ?
            ORDER BY s.student_id, sub.subject_id;
            """;

        String query2 = "SELECT student_id, year_section FROM students WHERE student_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             PreparedStatement stmt2 = conn.prepareStatement(query2)) {

            if (sessionStudentID == null || sessionStudentID.isEmpty()) {
                logger.error("Session faculty ID is null or empty");
                return;
            }
            int studentID = 0;
            String studentYearSection = "";
            stmt2.setString(1, sessionStudentID);
            try (ResultSet rs = stmt2.executeQuery()) {
                while (rs.next()) {
                    studentID = rs.getInt("student_id");
                    studentYearSection = rs.getString("year_section");
                }
            }
            stmt.setInt(1, studentID);
            stmt.setString(2, studentYearSection);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Get values first
                    String scholasticStatus = rs.getString("scholastic_status");
                    String subCode = rs.getString("subject_code");
                    String subDescription = rs.getString("description");
                    String facultyName = rs.getString("faculty_name");
                    String units = rs.getString("units");
                    String sectionCode = rs.getString("year_section");
                    String finGrade = rs.getString("final_grade");
                    String gradeStatus = rs.getString("gradestat");

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
                    computeSemesterGPA();
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading school events", ex);
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
        String selectedYearSection = yearSectionComboBox.getValue();
        String yearPrefix = selectedYearSection.substring(0, 1);
        ObservableList<Grades> filteredGrades = FXCollections.observableArrayList();

        String query = "SELECT year_section FROM students WHERE student_number = ? AND year_section LIKE ?";

        String searchGrades = """
            SELECT DISTINCT ON (s.student_id, sub.subject_id)
            s.student_id,
            s.student_number,
            s.year_section,
            s.scholastic_status,
            sub.subject_id,
            sub.subject_code,
            sub.description,
            sub.units,
            fac.faculty_id,
            fac.faculty_number,
            fac.firstname || ' ' || fac.lastname AS faculty_name,
            g.grade_id,
            g.final_grade,
            g.gradestat
            FROM student_load sl
            JOIN students s ON sl.student_id = s.student_id
            JOIN faculty_load fl ON sl.load_id = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN subjects sub ON fl.subject_id = sub.subject_id
            JOIN schedule sch ON fl.load_id = sl.load_id
            JOIN room r ON sch.room_id = r.room_id
            LEFT JOIN grade g ON g.faculty_load = fl.load_id AND g.student_id = s.student_number
            WHERE s.student_id = ? AND fl.year_section = ? AND sl.semester = ?
            ORDER BY s.student_id, sub.subject_id
           """;

        String searchStudentID = """
            SELECT student_id
            FROM students
            WHERE student_number = ?
            LIMIT 1
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(searchStudentID);
             PreparedStatement stmt2 = conn.prepareStatement(query);
             PreparedStatement stmt3 = conn.prepareStatement(searchGrades)) {
            stmt1.setString(1, SessionData.getInstance().getStudentNumber());
            stmt2.setString(1, SessionData.getInstance().getStudentNumber());
            stmt2.setString(2, yearPrefix + "-%");
            int studentID = 0;
            try (ResultSet rs = stmt1.executeQuery()) {
                while (rs.next()) {
                    studentID = rs.getInt("student_id");
                }
            }

            String studentYearSection = null;
            try (ResultSet rs = stmt2.executeQuery()) {
                while (rs.next()) {
                    studentYearSection = rs.getString("year_section");
                }
            }

            stmt3.setInt(1, studentID);
            stmt3.setString(2, studentYearSection);
            stmt3.setString(3, selectedSemester);

            boolean hasResults = false;

            try (ResultSet rs = stmt3.executeQuery()) {
                while (rs.next()) {
                    hasResults = true;

                    String scholasticStatus = rs.getString("scholastic_status");
                    String subCode = rs.getString("subject_code");
                    String subDescription = rs.getString("description");
                    String facultyName = rs.getString("faculty_name");
                    String units = rs.getString("units");
                    String sectionCode = rs.getString("year_section");
                    String finGrade = rs.getString("final_grade");
                    String gradeStatus = rs.getString("gradestat");

                    Grades grade = new Grades(scholasticStatus, subCode, subDescription, facultyName, units, sectionCode, finGrade, gradeStatus);
                    filteredGrades.add(grade);
                }
            }
            studentsList.clear();
            if (!hasResults) {
                // Assuming studentsTable is a Label or Text UI component
                studentsTable.setItems(FXCollections.observableArrayList());
                studentsTable.setPlaceholder(new Label("No grades available"));
            } else {
                // If you want, clear the text or set it back to something else when grades exist
                studentsTable.setItems(filteredGrades);
            }
        } catch (SQLException e) {
            logger.error("Failed to load student year section", e);
        }
    }
}
