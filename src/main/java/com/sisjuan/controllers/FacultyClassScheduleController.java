package com.sisjuan.controllers;

import com.sisjuan.models.Schedule;
import com.sisjuan.utilities.DBConnection;
import com.sisjuan.utilities.SchoolYearAndSemester;
import com.sisjuan.utilities.SessionData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class FacultyClassScheduleController {

    // FXML components
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
    private static final Logger logger = LoggerFactory.getLogger(FacultyClassScheduleController.class);

    // Initialize method to set up the table and load data
    @FXML 
    private void initialize() {
        schedules.clear();
        scheduleTable.setEditable(false);

        loadSchedules();

        var columns = new TableColumn[]{subjCodeCell, subjDescriptionCell, sectionCell, scheduleCell, dayCell, roomCell};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        scheduleTable.setSelectionModel(null);

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
                    setGraphic(label);
                }
            }
        });
    }

    // Method to load schedules from the database for the current faculty
    private void loadSchedules() {
        String facultyIdString = SessionData.getInstance().getFacultyId();
        logger.info("FacultyClassScheduleController: Retrieved SessionData.facultyId: '{}'", facultyIdString);

        if (facultyIdString == null || facultyIdString.trim().isEmpty()) {
            logger.error("FacultyClassScheduleController: Session faculty_id is null or empty. Cannot load schedules.");
            scheduleTable.setPlaceholder(new Label("Unable to load schedule: Faculty identifier not found in session."));
            return; // Exit if no ID
        }

        int facultyIdInt;
        try {
            facultyIdInt = Integer.parseInt(facultyIdString);
        } catch (NumberFormatException e) {
            logger.error("FacultyClassScheduleController: Could not parse faculty_id '{}' to an integer.", facultyIdString, e);
            scheduleTable.setPlaceholder(new Label("Unable to load schedule: Invalid faculty identifier format."));
            return;
        }

        if (facultyIdInt == 0) { // Assuming 0 might still be an invalid ID if parsing succeeded but was '0'
            logger.error("FacultyClassScheduleController: faculty_id is 0, which is considered invalid. Cannot load schedules.");
            scheduleTable.setPlaceholder(new Label("Unable to load schedule: Invalid faculty identifier."));
            return; 
        }

        String query = """
            SELECT CONCAT(s.subject_code, ' - ', s.description, ' (', sec.section_name, ')') AS faculty_description_section,
                   fac.faculty_id, fac.firstname || ' ' || fac.lastname AS faculty_name, fac.faculty_number,
                   fl.load_id, s.subject_id, s.subject_code, s.description AS subject_description,
                   sec.section_name AS year_section, sch.days,
                   TO_CHAR(sch.start_time, 'HH12:MI AM') AS start_time,
                   TO_CHAR(sch.end_time, 'HH12:MI AM') AS end_time,
                   r.room_name AS room, s.units, sch.lecture_hour, sch.laboratory_hour,
                   sem.semester_name
            FROM faculty_load fl
            LEFT JOIN schedule sch ON sch.faculty_load_id = fl.load_id
            JOIN faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN subjects s ON fl.subject_id = s.subject_id
            LEFT JOIN room r ON sch.room_id = r.room_id
            JOIN section sec ON fl.section_id = sec.section_id
            JOIN semesters sem ON fl.semester_id = sem.semester_id
            WHERE fac.faculty_id = ? AND fl.semester_id = ?;
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, facultyIdInt);
            int currentSemesterId = SchoolYearAndSemester.getCurrentSemesterId(); 
            if (currentSemesterId == 0) { 
                logger.error("FacultyClassScheduleController: Could not determine current semester ID.");
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
                    String units = rs.getString("units"); // units are text in the subject table
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

    // Helper method to get integer faculty_id from faculty_number string
    private int getIntegerFacultyIdByFacultyNumber(String facultyNumber) {
        String sql = "SELECT faculty_id FROM faculty WHERE faculty_number = ?";
        logger.debug("Attempting to fetch integer faculty_id for faculty_number: {}", facultyNumber);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, facultyNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int facultyId = rs.getInt("faculty_id");
                    logger.info("Successfully fetched integer faculty_id: {} for faculty_number: {}", facultyId, facultyNumber);
                    return facultyId;
                } else {
                    logger.warn("No faculty record found for faculty_number: {}", facultyNumber);
                    return 0; // Or throw a custom exception
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error while fetching faculty_id for faculty_number: {}. Error: {}", facultyNumber, e.getMessage(), e);
            return 0; // Or throw
        }
    }
}
