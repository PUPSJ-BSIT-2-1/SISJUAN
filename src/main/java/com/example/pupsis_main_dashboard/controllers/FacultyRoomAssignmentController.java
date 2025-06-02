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
        String query = """
                    SELECT CONCAT(faculty_number, ' - ', description, ' (', year_section, ')') AS faculty, fac.faculty_id, fac.firstname || ' ' || fac.lastname AS faculty_name, fac.faculty_number, fl.load_id, sub.subject_id, sub.subject_code, sub.description,
                           fl.year_section, sch.days, TO_CHAR(sch.start_time, 'HH:MI AM') AS start_time,
                           TO_CHAR(sch.end_time, 'HH:MI AM') AS end_time, r.room_name AS room, sub.units, sch.lecture_hour, sch.laboratory_hour
                    FROM schedule sch
                    JOIN faculty_load fl ON sch.faculty_load_id = fl.load_id
                    JOIN faculty fac ON fl.faculty_id = fac.faculty_id
                    JOIN subjects sub ON fl.subject_id = sub.subject_id
                    JOIN room r ON sch.room_id = r.room_id
                    WHERE fac.faculty_id = ? AND fl.semester = ?;
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (sessionFacultyID == null || sessionFacultyID.isEmpty()) {
                logger.error("Session faculty ID is null or empty");
                return;
            }

            stmt.setInt(1, Integer.parseInt(sessionFacultyID));
            stmt.setString(2, SchoolYearAndSemester.determineCurrentSemester());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Button editBtn = new Button("Edit");
                    editBtn.getStyleClass().add("edit-button");
                    // Get values first
                    int loadID = rs.getInt("load_id");
                    String faculty = rs.getString("faculty");
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
                            loadID, faculty, subjectID, facultyNumber, subCode, description, facultyName, facultyID, yearSection, days,
                            startTime, endTime, room, units, lectureHour, labHour, editBtn
                    );

                    schedules.add(schedule);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading school events", ex);
        }
    }

}



