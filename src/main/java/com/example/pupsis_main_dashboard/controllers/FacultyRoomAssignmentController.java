package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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

public class FacultyRoomAssignmentController {

    // FXML components 
    @FXML
    private VBox root;
    @FXML
    private TableView<Schedule> scheduleTable;
    @FXML
    private TableColumn<Schedule, String> subjCodeCell;
    @FXML
    private TableColumn<Schedule, String> subjDescriptionCell;
    @FXML
    private TableColumn<Schedule, String> sectionCell;
    @FXML
    private TableColumn<Schedule, String> scheduleCell;
    @FXML
    private TableColumn<Schedule, String> dayCell;
    @FXML
    private TableColumn<Schedule, String> roomCell;
    @FXML
    private Label semester;

    private final ObservableList<Schedule> schedules = FXCollections.observableArrayList();
    private static final Logger logger = LoggerFactory.getLogger(FacultyRoomAssignmentController.class);

    // Initialize method to set up the table and load data
    @FXML 
    private void initialize() {
        schedules.clear();
        scheduleTable.setEditable(false);

        loadSchedules();

        var columns = new TableColumn[]{subjCodeCell, subjDescriptionCell, sectionCell, scheduleCell, dayCell, roomCell};
        for (var col : columns) {
            col.setReorderable(false);
        }

        subjCodeCell.setCellValueFactory(new PropertyValueFactory<>("subCode"));
        subjDescriptionCell.setCellValueFactory(new PropertyValueFactory<>("subDesc"));
        sectionCell.setCellValueFactory(new PropertyValueFactory<>("yearSection"));
        scheduleCell.setCellValueFactory(new PropertyValueFactory<>("scheduleForFaculty"));
        dayCell.setCellValueFactory(new PropertyValueFactory<>("days"));
        roomCell.setCellValueFactory(new PropertyValueFactory<>("room"));

        scheduleTable.setItems(schedules);

        if (schedules.isEmpty()) {
            scheduleTable.setPlaceholder(new Label("No schedule available."));
        } else {
            scheduleTable.setPlaceholder(new Label("Loading data..."));
        }

        String acadYear = SchoolYearAndSemester.getCurrentAcademicYear();
        String sem = SchoolYearAndSemester.determineCurrentSemester();
        semester.setText("Academic Year " + acadYear + " - " + sem);

        for (TableColumn<Schedule, String> col : Arrays.asList(subjCodeCell, subjDescriptionCell, sectionCell, scheduleCell, dayCell, roomCell)) {
            setWrappingHeaderCellFactory(col);
        }

        scheduleTable.setRowFactory(_ -> {
            TableRow<Schedule> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });
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

            // Override the updateItem method to set the text of the label
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

    // Method to load schedules from the database for the current faculty
    private void loadSchedules() {
        String sessionFacultyID = SessionData.getInstance().getFacultyId();
        logger.info("FacultyRoomAssignmentController: Retrieved SessionData.facultyId: '{}'", sessionFacultyID);

        String query = """
            SELECT CONCAT(s.subject_code, ' - ', s.description, ' (', cs.year_section, ')') AS faculty_description_section,
                   fac.faculty_id, fac.firstname || ' ' || fac.lastname AS faculty_name, fac.faculty_number,
                   fl.load_id, s.subject_id, s.subject_code, s.description AS subject_description,
                   cs.year_section AS year_section, sch.days,
                   TO_CHAR(sch.start_time, 'HH12:MI AM') AS start_time,
                   TO_CHAR(sch.end_time, 'HH12:MI AM') AS end_time,
                   r.room_name AS room, s.units, sch.lecture_hour, sch.laboratory_hour,
                   sem.semester_name
            FROM schedule sch
            JOIN faculty_load fl ON sch.faculty_load_id = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN subjects s ON fl.subject_id = s.subject_id
            JOIN room r ON sch.room_id = r.room_id
            JOIN year_section cs ON fl.section_id = cs.section_id
            JOIN semesters sem ON fl.semester_id = sem.semester_id
            WHERE fac.faculty_id = ? AND fl.semester_id = ?;
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (sessionFacultyID == null || sessionFacultyID.trim().isEmpty()) {
                logger.error("FacultyRoomAssignmentController: Session faculty ID is critically null or empty. Cannot load schedules. Value was: '{}'", sessionFacultyID);
                scheduleTable.setPlaceholder(new Label("Unable to load schedule: Faculty ID not found in session."));
                return; // Exit if no ID
            }

            int facultyIdInt;
            try {
                facultyIdInt = Integer.parseInt(sessionFacultyID);
            } catch (NumberFormatException e) {
                logger.error("FacultyRoomAssignmentController: Session faculty ID is not a valid integer. Cannot load schedules. Value was: '{}'", sessionFacultyID);
                scheduleTable.setPlaceholder(new Label("Unable to load schedule: Faculty ID is not a valid integer."));
                return; // Exit if not an integer
            }

            stmt.setInt(1, facultyIdInt);
            // Assuming SchoolYearAndSemester.getCurrentSemesterId() returns the current semester's integer ID
            // This method might need to be created if it doesn't exist.
            int currentSemesterId = SchoolYearAndSemester.getCurrentSemesterId(); 
            if (currentSemesterId == 0) { // Or some other indicator of an invalid/unfetchable ID
                logger.error("FacultyRoomAssignmentController: Could not determine current semester ID.");
                scheduleTable.setPlaceholder(new Label("Unable to load schedule: Current semester ID not found."));
                return;
            }
            stmt.setInt(2, currentSemesterId);

            try (ResultSet rs = stmt.executeQuery()) {
                schedules.clear(); // Clear previous data
                while (rs.next()) {
                    // Get values first
                    int loadID = rs.getInt("load_id");
                    String facultyDescriptionSection = rs.getString("faculty_description_section"); // Using the new alias
                    String subjectID = rs.getString("subject_id");
                    String facultyNumber = rs.getString("faculty_number");
                    String subCode = rs.getString("subject_code");
                    String subjectDescription = rs.getString("subject_description"); // Using the new alias
                    String facultyName = rs.getString("faculty_name");
                    String facultyID = rs.getString("faculty_id");
                    String yearSection = rs.getString("year_section");
                    String days = rs.getString("days");
                    String startTime = rs.getString("start_time");
                    String endTime = rs.getString("end_time");
                    String room = rs.getString("room");
                    String units = rs.getString("units"); // units is text in subjects table
                    int lectureHour = rs.getInt("lecture_hour");
                    int labHour = rs.getInt("laboratory_hour");

                    // The Schedule constructor expects 'faculty' (which was a concat) and 'description'.
                    // We now have 'facultyDescriptionSection' for the concat, and 'subjectDescription' for the subject's description.
                    // Adjust the Schedule object creation based on its constructor's needs.
                    // For now, I'll pass facultyDescriptionSection as 'faculty' and subjectDescription as 'subDesc'.
                    Schedule schedule = new Schedule(
                            loadID, facultyDescriptionSection, subjectID, facultyNumber, subCode, subjectDescription, 
                            facultyName, facultyID, yearSection, days,
                            startTime, endTime, room, units, lectureHour, labHour,
                            null // Added null for the editButton parameter
                    );
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            logger.error("SQL Error loading schedules: ", e);
            scheduleTable.setPlaceholder(new Label("Error loading schedule data from database."));
        } catch (Exception e) {
            logger.error("Unexpected error loading schedules: ", e);
            scheduleTable.setPlaceholder(new Label("An unexpected error occurred."));
        }

        if (schedules.isEmpty()) {
            scheduleTable.setPlaceholder(new Label("No schedule available for the current semester."));
        }
    }
}
