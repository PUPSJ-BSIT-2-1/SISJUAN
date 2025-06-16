package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Grades;
import com.example.pupsis_main_dashboard.models.Schedule;
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

public class StudentGradingModuleController {
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
    private final Logger logger = LoggerFactory.getLogger(StudentGradingModuleController.class);

    public void initialize() {
        studentsTable.setEditable(false);
        populateYearSection();
        populateSemester();
        studentsTable.setPlaceholder(new Label("Loading data...")); // Initial placeholder
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
            col.setSortable(false);
        }

        studentsTable.setRowFactory(_ -> {
            TableRow<Grades> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });

        for (TableColumn<Grades, String> col : Arrays.asList(subCode, subDescription, facultyName, sectionCode, finGrade, gradeStatus)) {
            setWrappingHeaderCellFactory(col);
        }

        semesterComboBox.setOnAction(_ -> filterGrades());
        yearSectionComboBox.setOnAction(_ -> filterGrades());
    }

    private void setWrappingHeaderCellFactory(TableColumn<Grades, String> column) {
        Label headerLabel = new Label();
        headerLabel.setWrapText(true);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setStyle("-fx-alignment: center; -fx-text-alignment: center;");
        StackPane headerPane = new StackPane(headerLabel);
        headerPane.setPrefHeight(Control.USE_COMPUTED_SIZE);
        headerPane.setAlignment(Pos.CENTER);

        column.setGraphic(headerPane);

        column.setCellFactory(_ -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setStyle("-fx-alignment: center; -fx-text-alignment: center;");

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
                    setGraphic(label.getParent());  // StackPane as parent
                }
            }
        });
    }

    private void loadGrades() {
        String sessionStudentID = SessionData.getInstance().getStudentNumber();
        String query = "SELECT DISTINCT ON (s.student_id, sub.subject_id, sl.academic_year_id, sl.semester_id) " +
            "s.student_id, s.student_number, sec.section_name AS section_code, " +
            "scs.status_name AS scholastic_status_name, sub.subject_id, sub.subject_code, " +
            "sub.description AS subject_description, sub.units, fac.faculty_id, fac.faculty_number, " +
            "fac.firstname || ' ' || fac.lastname AS faculty_name, g.grade_id, g.final_grade, " +
            "gs.status_name AS grade_status_name, sl.academic_year_id, sl.semester_id " +
            "FROM student_load sl " +
            "JOIN students s ON sl.student_pk_id = s.student_id " +
            "JOIN faculty_load fl ON sl.faculty_load = fl.load_id " +
            "JOIN faculty fac ON fl.faculty_id = fac.faculty_id " +
            "JOIN subjects sub ON fl.subject_id = sub.subject_id " +
            "JOIN section sec ON fl.section_id = sec.section_id " +
            "LEFT JOIN schedule sch ON sch.faculty_load_id = fl.load_id " +
            "LEFT JOIN room r ON sch.room_id = r.room_id " +
            "LEFT JOIN grade g ON g.faculty_load = fl.load_id AND g.student_pk_id = s.student_id AND g.academic_year_id = sl.academic_year_id " +
            "LEFT JOIN grade_statuses gs ON g.grade_status_id = gs.grade_status_id " +
            "LEFT JOIN scholastic_statuses scs ON s.scholastic_status_id = scs.scholastic_status_id " +
            "WHERE s.student_id = ? " +
            "ORDER BY s.student_id, sub.subject_id, sl.academic_year_id DESC, sl.semester_id DESC, g.grade_id DESC;";

        String query2 = "SELECT s.student_id, s.current_year_section_id, sec.section_name, sec.year_level, ss.status_name AS current_scholastic_status " +
            "FROM students s " +
            "LEFT JOIN section sec ON s.current_year_section_id = sec.section_id " +
            "LEFT JOIN scholastic_statuses ss ON s.scholastic_status_id = ss.scholastic_status_id " +
            "WHERE s.student_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             PreparedStatement stmt2 = conn.prepareStatement(query2)) {

            if (sessionStudentID == null || sessionStudentID.isEmpty()) {
                logger.error("Session student number is null or empty");
                javafx.application.Platform.runLater(() -> studentsTable.setPlaceholder(new Label("Student session not found.")));
                return;
            }
            int studentID;
            stmt2.setString(1, sessionStudentID);
            try (ResultSet rs2 = stmt2.executeQuery()) {
                if (rs2.next()) { 
                    studentID = rs2.getInt("student_id");
                    String currentSectionName = rs2.getString("section_name");
                    int studentActualYearLevel = rs2.getInt("year_level");

                    // Update labels on JavaFX Application Thread
                    final String finalCurrentScholasticStatus = rs2.getString("current_scholastic_status");
                    final int finalStudentActualYearLevel = studentActualYearLevel;
                    javafx.application.Platform.runLater(() -> {
                        yearLevel.setText(String.valueOf(finalStudentActualYearLevel));
                        determineScholasticStatus(finalCurrentScholasticStatus);
                    });
                } else {
                    logger.warn("No student found with student_number: {}", sessionStudentID);
                    final String finalSessionStudentID = sessionStudentID; // Effectively final for lambda
                    javafx.application.Platform.runLater(() -> studentsTable.setPlaceholder(new Label("Student not found: " + finalSessionStudentID)));
                    return;
                }
            }
            stmt.setInt(1, studentID);
            logger.debug("Executing loadGrades query with StudentID: {}", studentID);

            try (ResultSet rs = stmt.executeQuery()) {
                studentsList.clear();
                while (rs.next()) {
                    // Logic for final grade display
                    Object finalGradeObj = rs.getObject("final_grade");
                    String finalGradeDisplay = ""; // Default to blank
                    if (finalGradeObj != null) {
                        String finalGradeStr = finalGradeObj.toString();
                        try {
                            Double.parseDouble(finalGradeStr); // Check if numeric
                            finalGradeDisplay = finalGradeStr; // It's numeric, so display it
                        } catch (NumberFormatException e) {
                            // Non-numeric, keep the finalGradeDisplay as "" (blank)
                        }
                    }

                    studentsList.add(new Grades(
                            rs.getString("scholastic_status_name"),
                            rs.getString("subject_code"),
                            rs.getString("subject_description"),
                            rs.getString("faculty_name"),
                            rs.getString("units"),
                            rs.getString("section_code"), 
                            finalGradeDisplay, // Use the processed display string
                            rs.getString("grade_status_name") != null ? rs.getString("grade_status_name") : "No Grade"
                    ));
                }
                // Update UI (GPA and placeholder) on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    computeSemesterGPA(); 
                    if (studentsList.isEmpty()) {
                        studentsTable.setPlaceholder(new Label("No Grades available."));
                    } else {
                        studentsTable.setPlaceholder(null); // Remove the placeholder if data is loaded
                    }
                });
            } catch (SQLException e) {
                logger.error("Error loading grades: ", e);
                javafx.application.Platform.runLater(() -> studentsTable.setPlaceholder(new Label("Error loading grades.")));
            }
        } catch (SQLException e) {
            logger.error("Database connection error in loadGrades: ", e);
            javafx.application.Platform.runLater(() -> studentsTable.setPlaceholder(new Label("Database error.")));
        }
    }

    private void populateSemester() {
        ObservableList<String> semesters = FXCollections.observableArrayList(
            "1st Semester", "2nd Semester", "Summer Term"
        );
        semesterComboBox.setItems(semesters);
    }

    private void populateYearSection() {
        Task<ObservableList<String>> task = new Task<>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                ObservableList<String> sections = FXCollections.observableArrayList();
                sections.add("All Sections"); // Add the "All Sections" option first
                String sql = "SELECT DISTINCT year_level FROM section ORDER BY year_level; ";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int yearLevel = rs.getInt("year_level");
                        String yearLevelStr = switch (yearLevel) {
                            case 1 -> "1st Year";
                            case 2 -> "2nd Year";
                            case 3 -> "3rd Year";
                            case 4 -> "4th Year";
                            default -> "Unknown Year";
                        };
                        sections.add(yearLevelStr);
                    }
                }
                return sections;
            }
        };
        task.setOnSucceeded(_ -> yearSectionComboBox.setItems(task.getValue()));
        new Thread(task).start();
    }

    private void determineYearLevel(String yearSection) {
        // Not needed anymore, year level is now directly retrieved from the database
    }

    private void determineScholasticStatus(String status) {
        // Ensure the UI update is on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (status != null && !status.isEmpty()) {
                scholasticStatus.setText(status);
            } else {
                scholasticStatus.setText("N/A"); // Default if status is null or empty
            }
        });
    }

    public void computeSemesterGPA() {
        double totalUnits = 0;
        double totalGradePoints = 0;
        boolean hasIncompleteGrades = false;

        for (Grades grades : studentsList) {
            String finalGrade = grades.getFinalGrade();
            String unitStr = grades.getUnits();

            if (finalGrade == null || finalGrade.isEmpty() ||
                    unitStr == null || unitStr.isEmpty()) {
                hasIncompleteGrades = true;
                break;
            }

            try {
                double gradePoints = Double.parseDouble(finalGrade);
                int units = Integer.parseInt(unitStr);
                totalUnits += units;
                totalGradePoints += gradePoints * units;
            } catch (NumberFormatException e) {
                hasIncompleteGrades = true;
                break;
            }
        }

        if (hasIncompleteGrades || totalUnits == 0) {
            semesterGPA.setText("N/A");
            setGWA(0);
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
        String selectedSection = yearSectionComboBox.getValue();
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
            JOIN section sec ON fl.section_id = sec.section_id
            LEFT JOIN schedule sch ON sch.faculty_load_id = fl.load_id
            LEFT JOIN room r ON sch.room_id = r.room_id
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
        if (selectedSemester == null || selectedSection == null) {
            logger.info("Semester or Section not selected for filtering.");
            loadGrades(); 
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt1_searchStudentId = conn.prepareStatement(searchStudentID);
             PreparedStatement stmt3_searchGrades = conn.prepareStatement(searchGrades)) {

            stmt1_searchStudentId.setString(1, sessionStudentNumber);
            int studentID;
            try (ResultSet rs = stmt1_searchStudentId.executeQuery()) {
                if (rs.next()) {
                    studentID = rs.getInt("student_id");
                } else {
                    logger.warn("No student found with student_number: {} for filtering.", sessionStudentNumber);
                    studentsTable.setPlaceholder(new Label("Student not found."));
                    return;
                }
            }

            int selectedSectionId;
            try {
                selectedSectionId = Integer.parseInt(selectedSection);
            } catch (NumberFormatException e) {
                logger.error("Invalid section format in ComboBox: {}", selectedSection, e);
                studentsTable.setPlaceholder(new Label("Invalid section filter value."));
                return;
            }

            stmt3_searchGrades.setInt(1, studentID);
            stmt3_searchGrades.setObject(2, selectedSectionId, Types.INTEGER); 
            stmt3_searchGrades.setString(3, selectedSemester);    

            boolean hasResults = false;

            try (ResultSet rs = stmt3_searchGrades.executeQuery()) {
                while (rs.next()) {
                    hasResults = true;

                    // Logic for final grade display
                    Object finalGradeObj = rs.getObject("final_grade");
                    String finalGradeDisplay = ""; // Default to blank
                    if (finalGradeObj != null) {
                        String finalGradeStr = finalGradeObj.toString();
                        try {
                            Double.parseDouble(finalGradeStr); // Check if numeric
                            finalGradeDisplay = finalGradeStr; // It's numeric, so display it
                        } catch (NumberFormatException e) {
                            // Non-numeric, keep the finalGradeDisplay as "" (blank)
                        }
                    }

                    studentsList.add(new Grades(
                            rs.getString("scholastic_status_name"),
                            rs.getString("subject_code"),
                            rs.getString("subject_description"),
                            rs.getString("faculty_name"),
                            rs.getString("units"),
                            rs.getString("section_code"),
                            finalGradeDisplay, // Use the processed display string
                            rs.getString("grade_status_name") != null ? rs.getString("grade_status_name") : "No Grade"
                    ));
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
                     // determineYearLevel(firstGrade.getSectionCode()); // Not needed anymore
                }
                computeSemesterGPA(); 
            }
        } catch (SQLException e) {
            logger.error("Failed to filter grades", e);
            studentsTable.setPlaceholder(new Label("Error filtering grades."));
        }
    }
}
