package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
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

public class StudentClassScheduleController {

    // FXML components
    @FXML
    private TableView<Schedule> studentTable;
    @FXML
    private TableColumn<Schedule, String> subjCodeCell;
    @FXML
    private TableColumn<Schedule, String> subjDescriptionCell;
    @FXML
    private TableColumn<Schedule, String> lecHourCell;
    @FXML
    private TableColumn<Schedule, String> labHourCell;
    @FXML
    private TableColumn<Schedule, String> unitsCell;
    @FXML
    private TableColumn<Schedule, String> scheduleCell;
    @FXML
    private TableColumn<Schedule, String> roomCell;
    @FXML
    private VBox root;
    @FXML
    private Label semester;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private static final Logger logger = LoggerFactory.getLogger(StudentClassScheduleController.class);

    // Initialize method to set up the table and load data
    @FXML
    public void initialize() {
        studentTable.setEditable(false);
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                loadSchedules();
                return null;
            }
        };
        new Thread(loadTask).start();

        // Set up the table columns
        subjCodeCell.setCellValueFactory(new PropertyValueFactory<>("subCode"));
        subjDescriptionCell.setCellValueFactory(new PropertyValueFactory<>("subDesc"));
        lecHourCell.setCellValueFactory(new PropertyValueFactory<>("stringLectureHour"));
        labHourCell.setCellValueFactory(new PropertyValueFactory<>("stringLaboratoryHour"));
        unitsCell.setCellValueFactory(new PropertyValueFactory<>("stringUnits"));
        scheduleCell.setCellValueFactory(new PropertyValueFactory<>("scheduleWithFaculty"));
        roomCell.setCellValueFactory(new PropertyValueFactory<>("room"));

        var columns = new TableColumn[]{subjCodeCell, subjDescriptionCell, lecHourCell, labHourCell, unitsCell, scheduleCell, roomCell};
        for (var col : columns) {
            col.setReorderable(false);
        }
        if (scheduleList.isEmpty()) {
            studentTable.setPlaceholder(new Label("No schedule available."));
        } else {
            studentTable.setPlaceholder(new Label("Loading data..."));
        }

        String acadYear = SchoolYearAndSemester.getCurrentAcademicYear();
        String sem = SchoolYearAndSemester.determineCurrentSemester();
        semester.setText("Academic Year " + acadYear + " - " + sem);

        // Set the wrapping header cell factory for each column
        for (TableColumn<Schedule, String> col : Arrays.asList(subjCodeCell, subjDescriptionCell, lecHourCell, labHourCell, unitsCell, scheduleCell, roomCell)) {
            setWrappingHeaderCellFactory(col);
        }

        // Set row height
        studentTable.setRowFactory(_ -> {
            TableRow<Schedule> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });

        studentTable.setItems(scheduleList);
    }

    // Method to set a custom cell factory for wrapping text in header cells
    private void setWrappingHeaderCellFactory(TableColumn<Schedule, String> column) {

        AtomicBoolean isDarkTheme = new AtomicBoolean(root.getScene() != null && root.getScene().getRoot().getStyleClass().contains("dark-theme"));
        root.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                isDarkTheme.set(newScene.getRoot().getStyleClass().contains("dark-theme"));
                newScene.getRoot().getStyleClass().addListener((ListChangeListener<String>) change ->
                        isDarkTheme.set(change.getList().contains("dark-theme")));
            }
        });

        // Set the cell factory for the column to wrap text and center it
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

    // Method to load schedules from the database
    private void loadSchedules() {
        String sessionStudentNumber = SessionData.getInstance().getStudentNumber();
        logger.debug("loadSchedules: Attempting to load schedules for student_number: {}", sessionStudentNumber);

        // SQL query to fetch schedules for the student
        String queryMainSchedule = """
            SELECT
                s.student_id,
                s.student_number,
                sub.subject_id,
                sub.subject_code,
                sub.description,
                sub.units,
                sch.days,
                TO_CHAR(sch.start_time, 'HH12:MI AM') AS start_time,
                TO_CHAR(sch.end_time, 'HH12:MI AM') AS end_time,
                COALESCE(r.room_name, 'TBA') AS room,
                fl.load_id,
                fac.faculty_id,
                fac.faculty_number,
                fac.firstname || ' ' || fac.lastname AS faculty_name,
                sch.lecture_hour,
                sch.laboratory_hour
            FROM public.student_load sl
            JOIN public.students s ON sl.student_pk_id = s.student_id
            JOIN public.subjects sub ON sl.subject_id = sub.subject_id
            JOIN public.faculty_load fl ON sl.faculty_load = fl.load_id
            JOIN public.faculty fac ON fl.faculty_id = fac.faculty_id
            LEFT JOIN public.schedule sch ON fl.load_id = sch.faculty_load_id
            LEFT JOIN public.room r ON sch.room_id = r.room_id
            WHERE sl.student_pk_id = ? AND fl.section_id = ?
            ORDER BY sub.subject_code, sch.start_time;
            """;

        // SQL query to get student ID and their current_year_section_id
        String queryStudentInfo = "SELECT student_id, current_year_section_id FROM public.students WHERE student_number = ?";

        if (sessionStudentNumber == null || sessionStudentNumber.isEmpty()) {
            logger.error("loadSchedules: Student number from session is null or empty. Cannot load schedules.");
            javafx.application.Platform.runLater(() -> studentTable.setPlaceholder(new Label("Could not identify student.")));
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmtStudentInfo = conn.prepareStatement(queryStudentInfo);
             PreparedStatement stmtMainSchedule = conn.prepareStatement(queryMainSchedule)) {

            int studentPkId = 0;
            int studentCurrentYearSectionId = 0;

            stmtStudentInfo.setString(1, sessionStudentNumber);
            logger.debug("loadSchedules: Executing student info query with student_number: {}", sessionStudentNumber);
            try (ResultSet rsStudentInfo = stmtStudentInfo.executeQuery()) {
                if (rsStudentInfo.next()) {
                    studentPkId = rsStudentInfo.getInt("student_id");
                    studentCurrentYearSectionId = rsStudentInfo.getInt("current_year_section_id");
                    logger.debug("loadSchedules: Found student_id: {}, current_year_section_id: {}", studentPkId, studentCurrentYearSectionId);
                } else {
                    logger.warn("loadSchedules: No student found with student_number: {}. Cannot load schedules.", sessionStudentNumber);
                    javafx.application.Platform.runLater(() -> studentTable.setPlaceholder(new Label("Student record not found.")));
                    return;
                }
            }

            if (studentPkId == 0 || studentCurrentYearSectionId == 0) {
                logger.error("loadSchedules: studentPkId or studentCurrentYearSectionId is 0, which is invalid. Aborting schedule load.");
                javafx.application.Platform.runLater(() -> studentTable.setPlaceholder(new Label("Student section info missing.")));
                return;
            }

            stmtMainSchedule.setInt(1, studentPkId);
            stmtMainSchedule.setInt(2, studentCurrentYearSectionId);
            logger.debug("loadSchedules: Executing main schedule query with student_id: {} and section_id: {}", studentPkId, studentCurrentYearSectionId);

            ObservableList<Schedule> localScheduleList = FXCollections.observableArrayList();
            try (ResultSet rsMain = stmtMainSchedule.executeQuery()) {
                while (rsMain.next()) {
                    String subjectCode = rsMain.getString("subject_code");
                    String subjectDescription = rsMain.getString("description");
                    int units = rsMain.getInt("units");
                    String days = rsMain.getString("days");
                    String startTime = rsMain.getString("start_time");
                    String endTime = rsMain.getString("end_time");
                    String room = rsMain.getString("room");
                    String facultyName = rsMain.getString("faculty_name");
                    int lectureHour = rsMain.getInt("lecture_hour");
                    int laboratoryHour = rsMain.getInt("laboratory_hour");

                    Schedule schedule = new Schedule(subjectCode, subjectDescription, units, days, startTime, endTime, room, facultyName, lectureHour, laboratoryHour);
                    localScheduleList.add(schedule);
                }
            }
            logger.debug("loadSchedules: Found {} schedule entries.", localScheduleList.size());

            javafx.application.Platform.runLater(() -> {
                scheduleList.setAll(localScheduleList);
                if (scheduleList.isEmpty()) {
                    studentTable.setPlaceholder(new Label("No schedule available for your section."));
                } else {
                    studentTable.setPlaceholder(null); // Remove placeholder if data is loaded
                }
            });

        } catch (SQLException e) {
            logger.error("loadSchedules: SQL Error loading schedules for student {}: ", sessionStudentNumber, e);
            javafx.application.Platform.runLater(() -> studentTable.setPlaceholder(new Label("Error loading schedule.")));
        } catch (Exception e) {
            logger.error("loadSchedules: Unexpected error loading schedules for student {}: ", sessionStudentNumber, e);
            javafx.application.Platform.runLater(() -> studentTable.setPlaceholder(new Label("Unexpected error.")));
        }
    }
}
