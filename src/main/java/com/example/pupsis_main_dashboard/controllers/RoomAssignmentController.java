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

public class RoomAssignmentController {

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
    private static final Logger logger = LoggerFactory.getLogger(RoomAssignmentController.class);

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
        String sessionStudentID = SessionData.getInstance().getStudentNumber();
        System.out.println("STUDENT_ID:" + sessionStudentID);
        
        // SQL query to fetch schedules for the student
        String query = """
            SELECT
                s.student_id,
                s.student_number,
                s.year_section,
                sub.subject_id,
                sub.subject_code,
                sub.description,
                sub.units,
                sch.days,
                TO_CHAR(sch.start_time, 'HH:MI AM') AS start_time,
                TO_CHAR(sch.end_time, 'HH:MI AM') AS end_time,
                r.room_name AS room,
                fl.load_id,
                fac.faculty_id,
                fac.faculty_number,
                fac.firstname || ' ' || fac.lastname AS faculty_name,
                sch.lecture_hour,
                sch.laboratory_hour
            FROM student_load sl
            JOIN students s ON sl.student_id = s.student_id
            JOIN subjects sub ON sl.subject_id = sub.subject_id
            JOIN faculty_load fl ON sl.faculty_load = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            LEFT JOIN schedule sch ON fl.load_id = sch.faculty_load_id
            LEFT JOIN room r ON sch.room_id = r.room_id
            WHERE s.student_id = ? AND fl.year_section = ?
            ORDER BY s.student_id, sub.subject_id, sch.start_time;
            """;

        // SQL query to get student ID and year section based on student number
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
                    int loadID = rs.getInt("load_id");
                    String subjectID = rs.getString("subject_id");
                    String facultyNumber = rs.getString("faculty_number");
                    String subCode = rs.getString("subject_code");
                    String description = rs.getString("description");
                    String facultyName = rs.getString("faculty_name");
                    String facultyID = rs.getString("faculty_id");
                    String yearSection = rs.getString("year_section");
                    String days = rs.getString("days");
                    String startTime = rs.getString("start_time");
                    String endTime = rs.getString("end_time");
                    String room = rs.getString("room");
                    int units = rs.getInt("units");
                    int lectureHour = rs.getInt("lecture_hour");
                    int labHour = rs.getInt("laboratory_hour");

                    Schedule schedule = new Schedule(
                            loadID, null, subjectID, facultyNumber, subCode, description, facultyName, facultyID, yearSection, days,
                            startTime, endTime, room, units, lectureHour, labHour, null
                    );

                    scheduleList.add(schedule);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading school events", ex);
        }
    }
}


