package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.NotificationUtil;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminClassScheduleController {

    // Variables for create schedule container
    @FXML
    private BorderPane borderPane;
    @FXML
    private Label scheduleHeader;
    @FXML
    private VBox scheduleContainer;
    @FXML
    private ComboBox<String> facultyIDComboBox;
    @FXML
    private TextField lectureHourTextField;
    @FXML
    private TextField laboratoryHourTextField;
    @FXML
    private ComboBox<String> roomComboBox;
    @FXML
    private ComboBox<String> startTimeComboBox;
    @FXML
    private ComboBox<String> endTimeComboBox;
    @FXML
    private CheckBox monCheckBox;
    @FXML
    private CheckBox tueCheckBox;
    @FXML
    private CheckBox wedCheckBox;
    @FXML
    private CheckBox thuCheckBox;
    @FXML
    private CheckBox friCheckBox;
    @FXML
    private CheckBox satCheckBox;
    @FXML
    private CheckBox sunCheckBox;
    @FXML
    private Button updateCancelButton;
    @FXML
    private Button createCancelButton;
    @FXML
    private Button createButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private HBox createScheduleButtonsContainer;
    @FXML
    private HBox updateScheduleButtonsContainer;

    // Variables for the main container
    @FXML
    private VBox vBox;
    @FXML
    private Label academicPeriod;
    @FXML
    private ComboBox<String> filterFacultyComboBox;
    @FXML
    private ComboBox<String> filterRoomComboBox;
    @FXML
    private HBox addSchedule;
    @FXML
    private HBox importRooms;

    // Variables for the schedule table
    @FXML
    private TableView<Schedule> scheduleTable;
    @FXML
    private TableColumn<Schedule, String> subCodeCell;
    @FXML
    private TableColumn<Schedule, String> subDescriptionCell;
    @FXML
    private TableColumn<Schedule, String> facultyNameCell;
    @FXML
    private TableColumn<Schedule, String> facultyIDCell;
    @FXML
    private TableColumn<Schedule, String> scheduleCell;
    @FXML
    private TableColumn<Schedule, String> roomCell;
    @FXML
    private TableColumn<Schedule, Button> editCell;

    private final ObservableList<Schedule> schedules = FXCollections.observableArrayList();
    private final List<Schedule> allSchedules = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(AdminClassScheduleController.class);

    // This method is called when the FXML file is loaded
    // and initializes the controller.
    @FXML
    private void initialize() {
        vBox.toFront();
        scheduleContainer.setOpacity(0);
        scheduleTable.setEditable(false);
        
        // Create a task to load schedules in the background
        Task<Void> task = getVoidTask();
        new Thread(task).start();

        var columns = new TableColumn[]{subCodeCell, subDescriptionCell, facultyNameCell, facultyIDCell, scheduleCell, roomCell, editCell};
        for (var col : columns) {
            col.setReorderable(false);
            col.setSortable(false);
        }

        scheduleTable.setSelectionModel(null);

        // Set cell value factories for each column
        facultyNameCell.setCellValueFactory(new PropertyValueFactory<>("facultyName"));
        facultyIDCell.setCellValueFactory(new PropertyValueFactory<>("facultyID"));
        subCodeCell.setCellValueFactory(new PropertyValueFactory<>("subCode"));
        subDescriptionCell.setCellValueFactory(new PropertyValueFactory<>("subDesc"));
        scheduleCell.setCellValueFactory(new PropertyValueFactory<>("schedule"));
        roomCell.setCellValueFactory(new PropertyValueFactory<>("room"));
        editCell.setCellValueFactory(new PropertyValueFactory<>("editButton"));

        scheduleTable.setItems(schedules);

        if (schedules.isEmpty()) {
            scheduleTable.setPlaceholder(new Label("No schedule available."));
        } else {
            scheduleTable.setPlaceholder(new Label("Loading data..."));
        }

        String acadYear = SchoolYearAndSemester.getCurrentAcademicYear();
        String sem = SchoolYearAndSemester.determineCurrentSemester();
        academicPeriod.setText("Academic Year " + acadYear + " - " + sem);

        for (TableColumn<Schedule, String> col : Arrays.asList(subCodeCell, subDescriptionCell, facultyNameCell, facultyIDCell, scheduleCell, roomCell)) {
            setWrappingHeaderCellFactory(col);
        }

        scheduleTable.setRowFactory(_ -> {
            TableRow<Schedule> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });

        setupTimeComboBoxListeners();
        filterFacultyComboBox.setOnAction(_ -> applyFilters());
        filterRoomComboBox.setOnAction(_ -> applyFilters());

        updateCancelButton.setOnAction(_ -> handleCancelSchedule());
        createCancelButton.setOnAction(_ -> handleCancelSchedule());
        addSchedule.setOnMouseClicked(_ -> displayCreateScheduleForm());
        importRooms.setOnMouseClicked(_ -> handleImportRoomCSV());
    }

    private void displayCreateScheduleForm() {
        borderPane.toFront(); 
        scheduleContainer.setDisable(false);
        scheduleContainer.setOpacity(1);
        clearAllFields();
        showCreateButtonsContainer();
        facultyIDComboBox.setDisable(false);
        scheduleHeader.setText("Create Schedule");
    }

    // This method creates a Task that loads schedules and populates the combo boxes.
    private Task<Void> getVoidTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    loadSchedules();
                    // Populate ComboBoxes on a separate thread after loading schedules
                    populateFacultyIDComboBox();
                    populateFilterFacultyComboBox();
                    populateFilterRoomComboBox(); // This also populates roomComboBox for selection
                    populateTimeComboBox();
                } catch (Exception e) {
                    logger.error("Error during initial data loading task", e);
                }
                return null;
            }
        };
    }

    // This method loads schedules from the database and populates the table.
    private void loadSchedules() {
        schedules.clear();
        allSchedules.clear();
        String currentSemesterName = SchoolYearAndSemester.determineCurrentSemester();
        int semesterId = SchoolYearAndSemester.getSemesterId(currentSemesterName);
        int academicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();

        String query = """
            SELECT
                s.schedule_id, fl.load_id, sub.subject_code, sub.description AS subject_description,
                CONCAT(f.firstname, ' ', f.lastname) AS faculty_name, f.faculty_number,
                r.room_name, s.start_time, s.end_time, s.days, sub.units,
                s.lecture_hour, s.laboratory_hour,
                sec.section_name 
            FROM public.schedule s
            JOIN public.faculty_load fl ON s.faculty_load_id = fl.load_id
            JOIN public.subjects sub ON fl.subject_id = sub.subject_id
            JOIN public.faculty f ON fl.faculty_id = f.faculty_id
            LEFT JOIN public.room r ON s.room_id = r.room_id
            JOIN public.section sec ON fl.section_id = sec.section_id 
            WHERE fl.semester_id = ? AND fl.academic_year_id = ?
            ORDER BY s.schedule_id;
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, semesterId);
            pstmt.setInt(2, academicYearId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    Button editButton = new Button("Edit");
                    editButton.getStyleClass().add("edit-button");
                    int loadId = rs.getInt("load_id");
                    String subjectCode = rs.getString("subject_code"); 
                    String facultyNumber = rs.getString("faculty_number");
                    String subDesc = rs.getString("subject_description");
                    String facultyName = rs.getString("faculty_name");
                    String sectionName = rs.getString("section_name"); 
                    String days = rs.getString("days");
                    Time startTimeSql = rs.getTime("start_time");
                    Time endTimeSql = rs.getTime("end_time");
                    String startTime = (startTimeSql != null) ? new SimpleDateFormat("hh:mm a").format(startTimeSql) : "";
                    String endTime = (endTimeSql != null) ? new SimpleDateFormat("hh:mm a").format(endTimeSql) : "";
                    String room = rs.getString("room_name");
                    String unitsStr = rs.getString("units");
                    int lectureHour = rs.getInt("lecture_hour");
                    int laboratoryHour = rs.getInt("laboratory_hour");

                    String facultyDisplayValue = facultyName + " (" + facultyNumber + ")";

                    Schedule schedule = new Schedule(loadId, facultyDisplayValue, subjectCode, facultyNumber, subjectCode, subDesc, facultyName, facultyNumber, sectionName, days, startTime, endTime, room, unitsStr, lectureHour, laboratoryHour, editButton);
                    createButton.setOnAction(_ -> handleCreateSchedule());
                    editButton.setOnAction(_ -> handleEditSchedule(schedule, borderPane, scheduleContainer));
                    deleteButton.setOnAction(_ -> handleDeleteSchedule(schedule));
                    schedules.add(schedule);
                    allSchedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load schedules", e);
            StageAndSceneUtils.showAlert("Database Error", "Could not load schedules. " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // This method sets a custom header cell factory for the specified column to allow wrapping text.
    private void setWrappingHeaderCellFactory(TableColumn<Schedule, String> column) {
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

    // This method populates the faculty ID combo box with faculty load information for unscheduled loads.
    private void populateFacultyIDComboBox() {
        ObservableList<String> facultyLoadStrings = FXCollections.observableArrayList();
        String currentSemesterName = SchoolYearAndSemester.determineCurrentSemester();
        int semesterId = SchoolYearAndSemester.getSemesterId(currentSemesterName);
        int academicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();

        facultyIDComboBox.getItems().clear();
        String query = """
                SELECT CONCAT(fac.faculty_number, ' - ', sub.description, ' (', sec.section_name, ')') AS faculty_load_display
                FROM public.faculty_load fl
                JOIN public.faculty fac ON fl.faculty_id = fac.faculty_id
                JOIN public.subjects sub ON fl.subject_id = sub.subject_id
                JOIN public.section sec ON fl.section_id = sec.section_id 
                WHERE fl.semester_id = ? AND fl.academic_year_id = ? AND fl.load_id NOT IN (SELECT faculty_load_id FROM schedule)
                ORDER BY faculty_load_display;
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, semesterId);
            pstmt.setInt(2, academicYearId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    facultyLoadStrings.add(rs.getString("faculty_load_display"));
                }
                facultyIDComboBox.setItems(facultyLoadStrings);
            }
        } catch (SQLException e) {
            logger.error("Failed to populate faculty ID combo box", e);
            StageAndSceneUtils.showAlert("Database Error", "Could not load faculty IDs for selection. " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // This method populates the start and end time combo boxes with time slots.
    private void populateTimeComboBox() {
        ObservableList<String> times = FXCollections.observableArrayList();
        LocalTime time = LocalTime.of(7, 0); 
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        while (time.isBefore(LocalTime.of(21, 30))) { 
            times.add(time.format(formatter));
            time = time.plusMinutes(30);
        }
        startTimeComboBox.setItems(times);
        endTimeComboBox.setItems(times);
    }

    // This method populates the filter faculty combo box with distinct faculty names.
    private void populateFilterFacultyComboBox() {
        ObservableList<String> facultyNames = FXCollections.observableArrayList();
        facultyNames.add("All Faculty"); // Add an option for no filter
        String query = "SELECT DISTINCT CONCAT(f.firstname, ' ', f.lastname) AS faculty_name FROM public.faculty f ORDER BY faculty_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                facultyNames.add(rs.getString("faculty_name"));
            }
            filterFacultyComboBox.setItems(facultyNames);
            if (!facultyNames.isEmpty()) {
                filterFacultyComboBox.getSelectionModel().selectFirst(); // Default to "All Faculty"
            }
        } catch (SQLException e) {
            logger.error("Failed to populate filter faculty combo box", e);
            StageAndSceneUtils.showAlert("Database Error", "Could not load faculty names for filtering.", Alert.AlertType.ERROR);
        }
    }

    // This method populates the filter room combo box with distinct room names.
    private void populateFilterRoomComboBox() {
        String query = "SELECT DISTINCT r.room_name FROM public.room r ORDER BY r.room_name";
        ObservableList<String> filterRoomNames = FXCollections.observableArrayList();
        filterRoomNames.add("All Rooms"); // Add an option for no filter
        ObservableList<String> selectionRoomNames = FXCollections.observableArrayList(); 

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String roomName = rs.getString("room_name");
                filterRoomNames.add(roomName);
                selectionRoomNames.add(roomName);
            }

            Platform.runLater(() -> {
                filterRoomComboBox.setItems(filterRoomNames);
                roomComboBox.setItems(selectionRoomNames);
                if (!filterRoomNames.isEmpty()) {
                    filterRoomComboBox.getSelectionModel().selectFirst(); // Default to "All Rooms"
                }
            });
        } catch (SQLException e) {
            logger.error("Failed to populate filter/selection room combo box", e);
        }
    }

    // This method retrieves the selected days from the checkboxes and returns them as a string.
    private String getSelectedDays() {
        StringBuilder sb = new StringBuilder();
        if (monCheckBox.isSelected()) sb.append("M");
        if (tueCheckBox.isSelected()) sb.append("T");
        if (wedCheckBox.isSelected()) sb.append("W");
        if (thuCheckBox.isSelected()) sb.append("Th");
        if (friCheckBox.isSelected()) sb.append("F");
        if (satCheckBox.isSelected()) sb.append("S");
        if (sunCheckBox.isSelected()) sb.append("Su");
        return sb.toString();
    }

    // This method handles the addition of a new schedule.
    @FXML
    private void handleCreateSchedule() {
        String facultyComboBoxValue = facultyIDComboBox.getValue();
        if (facultyComboBoxValue == null || facultyComboBoxValue.isEmpty()) {
            StageAndSceneUtils.showAlert("Input Error", "Please select a faculty load.", Alert.AlertType.ERROR);
            return;
        }
        int facultyLoadID = searchFacultyLoadID(facultyComboBoxValue);
        if (facultyLoadID == -1) {
            StageAndSceneUtils.showAlert("Data Error", "Selected faculty load could not be found or parsed correctly.", Alert.AlertType.WARNING);
            return;
        }

        String roomName = roomComboBox.getValue();
        String startTime = startTimeComboBox.getValue();
        String endTime = endTimeComboBox.getValue();
        String days = getSelectedDays();
        String lectureHourText = lectureHourTextField.getText();
        String laboratoryHourText = laboratoryHourTextField.getText();

        if (roomName == null || startTime == null || endTime == null || days.isEmpty() || lectureHourText.isEmpty() || laboratoryHourText.isEmpty()) {
            StageAndSceneUtils.showAlert("Input Error", "All fields must be filled.", Alert.AlertType.WARNING);
            return;
        }

        int lectureHourInt, laboratoryHourInt;
        try {
            lectureHourInt = Integer.parseInt(lectureHourText);
            laboratoryHourInt = Integer.parseInt(laboratoryHourText);
        } catch (NumberFormatException e) {
            StageAndSceneUtils.showAlert("Input Error", "Lecture or Laboratory hour must be a number.", Alert.AlertType.WARNING);
            return;
        }

        boolean isScheduleFree = isScheduleFree(roomName, startTime, endTime, days, facultyLoadID);
        if (!isScheduleFree) {
            StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Schedule is not free.");
            return;
        }

        String insertScheduleQuery = """
            INSERT INTO public.schedule (faculty_load_id, room_id, start_time, end_time, days, lecture_hour, laboratory_hour)
            VALUES (?, (SELECT room_id FROM public.room WHERE room_name = ?), TO_TIMESTAMP(?, 'HH12:MI AM'), TO_TIMESTAMP(?, 'HH12:MI AM'), ?, ?, ?)
            RETURNING schedule_id;
        """;

        String getInsertedScheduleQuery = """
            SELECT
                s.schedule_id, fl.load_id, sub.subject_code, sub.description AS subject_description,
                CONCAT(f.firstname, ' ', f.lastname) AS faculty_name, f.faculty_number,
                sub.units,sec.section_name 
            FROM public.schedule s
            JOIN public.faculty_load fl ON s.faculty_load_id = fl.load_id
            JOIN public.subjects sub ON fl.subject_id = sub.subject_id
            JOIN public.faculty f ON fl.faculty_id = f.faculty_id
            JOIN public.section sec ON fl.section_id = sec.section_id 
            WHERE s.schedule_id = ?;
        """;

// insert and get new schedule_id
        try (Connection conn = DBConnection.getConnection();
            PreparedStatement scheduleStmt = conn.prepareStatement(insertScheduleQuery)) {
            scheduleStmt.setInt(1, facultyLoadID);
            scheduleStmt.setString(2, roomName);
            scheduleStmt.setString(3, startTime);
            scheduleStmt.setString(4, endTime);
            scheduleStmt.setString(5, days);
            scheduleStmt.setInt(6, lectureHourInt);
            scheduleStmt.setInt(7, laboratoryHourInt);

            ResultSet rsInsert = scheduleStmt.executeQuery();
            if (rsInsert.next()) {
                int newScheduleId = rsInsert.getInt("schedule_id");

                try (PreparedStatement detailStmt = conn.prepareStatement(getInsertedScheduleQuery)) {
                    detailStmt.setInt(1, newScheduleId);
                    ResultSet rsDetail = detailStmt.executeQuery();
                    if (rsDetail.next()) {
                        Button editButton = new Button("Edit");
                        editButton.getStyleClass().add("edit-button");
                        int loadId = rsDetail.getInt("load_id");
                        String subjectCode = rsDetail.getString("subject_code");
                        String facultyNumber = rsDetail.getString("faculty_number");
                        String subDesc = rsDetail.getString("subject_description");
                        String facultyName = rsDetail.getString("faculty_name");
                        String sectionName = rsDetail.getString("section_name");
                        String unitsStr = rsDetail.getString("units");

                        String facultyDisplayValue = facultyName + " (" + facultyNumber + ")";

                        Schedule schedule = new Schedule(loadId, facultyDisplayValue, subjectCode, facultyNumber, subjectCode, subDesc, facultyName, facultyNumber, sectionName, days, startTime, endTime, roomName, unitsStr, lectureHourInt, laboratoryHourInt, editButton);
                        editButton.setOnAction(_ -> handleEditSchedule(schedule, borderPane, scheduleContainer));
                        deleteButton.setOnAction(_ -> handleDeleteSchedule(schedule));

                        schedules.add(schedule);
                        allSchedules.add(schedule);
                        scheduleTable.setItems(schedules);
                        scheduleTable.refresh();

                        Platform.runLater(this::populateFacultyIDComboBox);
                        StageAndSceneUtils.showAlert("Success", "Schedule added successfully!", Alert.AlertType.INFORMATION);
                        handleCancelSchedule(); // clear form
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to add schedule", e);
            StageAndSceneUtils.showAlert("Database Error", "Failed to add schedule.", Alert.AlertType.ERROR);
        }
    }

    // This method searches for the faculty load ID based on the selected faculty in the combo box.
    private int searchFacultyLoadID(String facultyComboBoxValue) {
        if (facultyComboBoxValue == null || facultyComboBoxValue.isEmpty()) {
            logger.warn("Faculty combo box value is null or empty in searchFacultyLoadID.");
            return -1; 
        }

        String[] parts = facultyComboBoxValue.split(" - | \\(|\\)");
        String facultyNumber = parts[0];
        String subjectDescription = parts[1];
        String sectionNameFull = parts[2];

        String query = """
            SELECT fl.load_id
            FROM public.faculty_load fl
            JOIN public.faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN public.subjects sub ON fl.subject_id = sub.subject_id
            JOIN public.section sec ON fl.section_id = sec.section_id
            WHERE fac.faculty_number = ? AND sub.description = ? AND sec.section_name = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, facultyNumber);
            pstmt.setString(2, subjectDescription);
            pstmt.setString(3, sectionNameFull);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("load_id");
            } else {
                logger.warn("Faculty load not found in DB for: FN='{}', Desc='{}', SN='{}'", facultyNumber, subjectDescription, sectionNameFull);
                return -1; 
            }
        } catch (SQLException e) {
            logger.error("Error searching faculty load ID for: {}", facultyComboBoxValue, e);
            return -1; 
        }
    }

    // This method handles the editing of an existing schedule.
    private void handleEditSchedule(Schedule schedule, BorderPane borderPane, VBox scheduleContainer) {
        borderPane.toFront();
        scheduleContainer.setDisable(false);
        scheduleContainer.setOpacity(1);
        showUpdateButtonsContainer();
        facultyIDComboBox.setDisable(true); 
        scheduleHeader.setText("Update Schedule");

        facultyIDComboBox.setValue(schedule.getFacultyNumber() + " - " + schedule.getFaculty());
        lectureHourTextField.setText(String.valueOf(schedule.getLectureHour()));
        laboratoryHourTextField.setText(String.valueOf(schedule.getLaboratoryHour()));
        roomComboBox.setValue(schedule.getRoom());
        startTimeComboBox.setValue(schedule.getStartTime());
        endTimeComboBox.setValue(schedule.getEndTime());

        String[] days = schedule.getDays().split("(?<=\\D)(?=M|T|W|Th|F|S|Su)");
        for (String day : days) {
            switch (day) {
                case "M"  -> monCheckBox.setSelected(true);
                case "T"  -> tueCheckBox.setSelected(true);
                case "W"  -> wedCheckBox.setSelected(true);
                case "Th" -> thuCheckBox.setSelected(true);
                case "F"  -> friCheckBox.setSelected(true);
                case "S"  -> satCheckBox.setSelected(true);
                case "Su" -> sunCheckBox.setSelected(true);
            }
        }

        updateButton.setOnAction(_ -> {
            int facultyLoadIDToUpdate = schedule.getLoadID(); 
            
            int updatedLectureHour, updatedLaboratoryHour;
            try {
                updatedLectureHour = Integer.parseInt(lectureHourTextField.getText());
                updatedLaboratoryHour = Integer.parseInt(laboratoryHourTextField.getText());
            } catch (NumberFormatException e) {
                StageAndSceneUtils.showAlert("Input Error", "Lecture or Laboratory hour must be a number.", Alert.AlertType.WARNING);
                return;
            }
            String updatedRoomName = roomComboBox.getValue();
            String updatedStartTime = startTimeComboBox.getValue();
            String updatedEndTime = endTimeComboBox.getValue();
            String updatedDays = getSelectedDays();

            if (updatedRoomName == null || updatedStartTime == null || updatedEndTime == null || updatedDays.isEmpty()) {
                StageAndSceneUtils.showAlert("Input Error", "All fields must be filled.", Alert.AlertType.WARNING);
                return;
            }

            boolean isScheduleFree = isScheduleFree(updatedRoomName, updatedStartTime, updatedEndTime, updatedDays, facultyLoadIDToUpdate);
            if (!isScheduleFree) {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Schedule is not free.");
                return;
            }

            String updateQuery = "UPDATE public.schedule SET room_id = (SELECT room_id FROM public.room WHERE room_name = ?), start_time = TO_TIMESTAMP(?, 'HH12:MI AM'), end_time = TO_TIMESTAMP(?, 'HH12:MI AM'), days = ?, lecture_hour = ?, laboratory_hour = ? WHERE faculty_load_id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmtUpdate = conn.prepareStatement(updateQuery)) {
                    pstmtUpdate.setString(1, updatedRoomName);
                    pstmtUpdate.setString(2, updatedStartTime);
                    pstmtUpdate.setString(3, updatedEndTime);
                    pstmtUpdate.setString(4, updatedDays);
                    pstmtUpdate.setInt(5, updatedLectureHour);
                    pstmtUpdate.setInt(6, updatedLaboratoryHour);
                    pstmtUpdate.setInt(7, facultyLoadIDToUpdate);
                    pstmtUpdate.executeUpdate();
                    StageAndSceneUtils.showAlert("Success", "Schedule updated successfully!", Alert.AlertType.INFORMATION);

                    // Update the schedule object
                    schedule.setLoadID(facultyLoadIDToUpdate);
                    schedule.setLectureHour(updatedLectureHour);
                    schedule.setLaboratoryHour(updatedLaboratoryHour);
                    schedule.setRoom(updatedRoomName);
                    schedule.setStartTime(updatedStartTime);
                    schedule.setEndTime(updatedEndTime);
                    schedule.setDays(updatedDays);
                    schedule.setSchedule(schedule.getYearSection() + " " + schedule.getDays() + " " + schedule.getStartTime() + " - " + schedule.getEndTime());

                    // Find and update the schedule in the list
                    int index = schedules.indexOf(schedule);
                    if (index != -1) {
                        schedules.set(index, schedule); // Replace the old one with the updated one
                    }

                    // Same for allSchedules if you're using it elsewhere
                    int allIndex = allSchedules.indexOf(schedule);
                    if (allIndex != -1) {
                        allSchedules.set(allIndex, schedule);
                    }

                    scheduleTable.setItems(schedules);
                    scheduleTable.refresh();
                    handleCancelSchedule();
            } catch (SQLException e) {
                logger.error("Failed to update schedule in the database", e);
                StageAndSceneUtils.showAlert("Database Error", "Failed to update schedule. It might conflict or violate constraints.", Alert.AlertType.ERROR);
            }// Return to the previous view
        });
    }

    // This method handles the deletion of an existing schedule.
    private void handleDeleteSchedule(Schedule schedule) {
        if (schedule == null) {
            StageAndSceneUtils.showAlert("Selection Error", "No schedule selected for deletion.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Confirm Deletion");
        confirmationDialog.setHeaderText("Delete Schedule");
        confirmationDialog.setContentText("Are you sure you want to delete the schedule for: " + schedule.getFaculty() + "?");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int facultyLoadID = schedule.getLoadID();
            String deleteQuery = "DELETE FROM public.schedule WHERE faculty_load_id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                pstmt.setInt(1, facultyLoadID);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    schedules.remove(schedule);
                    allSchedules.remove(schedule);
                    scheduleTable.setItems(schedules);
                    scheduleTable.refresh();
                    handleCancelSchedule();
                    Platform.runLater(this::populateFacultyIDComboBox);
                    // Show a success message
                    StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.INFORMATION), "Schedule deleted successfully!");
                } else {
                    StageAndSceneUtils.showAlert("Deletion Warning", "No schedule was deleted. The record may have been removed by another user or an error occurred.", Alert.AlertType.WARNING);
                }
            } catch (SQLException e) {
                logger.error("Failed to delete schedule with faculty_load_id: {}", facultyLoadID, e);
                StageAndSceneUtils.showAlert("Database Error", "Failed to delete schedule due to a database error.", Alert.AlertType.ERROR);
            }
        }
    }
    // This method sets up the listeners for the start and end time combo boxes.
    private void setupTimeComboBoxListeners() {
        ChangeListener<Object> timeChangeListener = (_, _, _) -> updateHourFields();

        startTimeComboBox.valueProperty().addListener(timeChangeListener);
        endTimeComboBox.valueProperty().addListener(timeChangeListener);
    }

    // This method updates the hour fields based on the selected start and end times.
    private void updateHourFields() {
        String startTime = startTimeComboBox.getValue();
        String endTime = endTimeComboBox.getValue();

        if (startTime == null || endTime == null) {
            lectureHourTextField.clear();
            laboratoryHourTextField.clear();
            return;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            LocalTime start = LocalTime.parse(startTime, formatter);
            LocalTime end = LocalTime.parse(endTime, formatter);

            long hours = Duration.between(start, end).toHours();

            if (hours == 5) {
                lectureHourTextField.setText("3");
                laboratoryHourTextField.setText("2");
            } else if (hours > 0 && hours < 5) {
                lectureHourTextField.setText(String.valueOf(hours));
                laboratoryHourTextField.setText("0");
            } else {
                lectureHourTextField.clear();
                laboratoryHourTextField.clear();
                StageAndSceneUtils.showAlert(Alert.AlertType.WARNING.toString(), "Invalid time selection. Duration must be between 1 and 5 hours.");
            }
        } catch (DateTimeParseException e) {
            lectureHourTextField.clear();
            laboratoryHourTextField.clear();
            StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.ERROR), "Invalid time format. Please ensure the time is correctly formatted.");
        }
    }

    // This method checks if the proposed schedule is free in the specified room.
    private boolean isScheduleFree(String room, String proposedStart, String proposedEnd, String proposedDays, int facultyLoadId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        LocalTime proposedStartTime = LocalTime.parse(proposedStart, formatter);
        LocalTime proposedEndTime = LocalTime.parse(proposedEnd, formatter);

        for (Schedule schedule : allSchedules) {
            if ((schedule.getRoom().equals(room) || schedule.getLoadID() == facultyLoadId)
                    && schedule.getLoadID() != facultyLoadId) {

                LocalTime existingStartTime = LocalTime.parse(schedule.getStartTime(), formatter);
                LocalTime existingEndTime = LocalTime.parse(schedule.getEndTime(), formatter);

                if (proposedStartTime.isBefore(existingEndTime) && proposedEndTime.isAfter(existingStartTime)) {
                    if (hasDayOverlap(schedule.getDays(), proposedDays)) {
                        return false; // Conflict found
                    }
                }
            }
        }
        return true; // No conflict
    }

    // This method checks if the existing days overlap with the proposed days.
    private boolean hasDayOverlap(String existingDays, String proposedDays) {
        Set<String> existingDaySet = extractDaysToSet(existingDays);
        Set<String> proposedDaySet = extractDaysToSet(proposedDays);

        return !Collections.disjoint(existingDaySet, proposedDaySet);
    }

    private Set<String> extractDaysToSet(String daysString) {
        Set<String> result = new HashSet<>();
        Matcher matcher = Pattern.compile("Th|Su|M|T|W|F|S").matcher(daysString);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }


    // This method handles the cancellation of the schedule creation or update.
    private void handleCancelSchedule() {
        vBox.toFront(); 
        scheduleContainer.setDisable(true);
        scheduleContainer.setOpacity(0);
        clearAllFields();
        showCreateButtonsContainer(); 
        facultyIDComboBox.setDisable(false); 
        scheduleHeader.setText("Create Schedule"); 
    }

    // This method shows the creation buttons container and hides the update buttons container.
    private void showCreateButtonsContainer() {
        createScheduleButtonsContainer.toFront();
        createScheduleButtonsContainer.setOpacity(1);
        createScheduleButtonsContainer.setDisable(false);
        updateScheduleButtonsContainer.toFront(); 
        updateScheduleButtonsContainer.setOpacity(0);
        updateScheduleButtonsContainer.setDisable(true);
    }

    // This method shows the update buttons container and hides the creation buttons container.
    private void showUpdateButtonsContainer() {
        updateScheduleButtonsContainer.toFront();
        updateScheduleButtonsContainer.setOpacity(1);
        updateScheduleButtonsContainer.setDisable(false);
        createScheduleButtonsContainer.toFront(); 
        createScheduleButtonsContainer.setOpacity(0);
        createScheduleButtonsContainer.setDisable(true);
    }

    // This method clears all input fields in the schedule creation form.
    private void clearAllFields() {
        facultyIDComboBox.getSelectionModel().clearSelection();
        facultyIDComboBox.setValue(null);
        lectureHourTextField.clear();
        laboratoryHourTextField.clear();
        roomComboBox.getSelectionModel().clearSelection();
        roomComboBox.setValue(null);
        startTimeComboBox.getSelectionModel().clearSelection();
        startTimeComboBox.setValue(null);
        endTimeComboBox.getSelectionModel().clearSelection();
        endTimeComboBox.setValue(null);
        monCheckBox.setSelected(false);
        tueCheckBox.setSelected(false);
        wedCheckBox.setSelected(false);
        thuCheckBox.setSelected(false);
        friCheckBox.setSelected(false);
        satCheckBox.setSelected(false);
        sunCheckBox.setSelected(false);
    }

    private void resetFilters() {
        filterFacultyComboBox.setValue("All Faculty");
        filterRoomComboBox.setValue("All Rooms");
        scheduleTable.setItems((ObservableList<Schedule>) allSchedules);
        scheduleTable.setPlaceholder(null);
    }

    private void applyFilters() {
        String selectedFaculty = filterFacultyComboBox.getValue();
        String selectedRoom = filterRoomComboBox.getValue();

        boolean filterByFaculty = selectedFaculty != null && !selectedFaculty.equals("All Faculty");
        boolean filterByRoom = selectedRoom != null && !selectedRoom.equals("All Rooms");

        ObservableList<Schedule> filteredSchedules = FXCollections.observableArrayList();

        for (Schedule scheduleItem : allSchedules) {
            boolean facultyMatch = !filterByFaculty || (scheduleItem.getFacultyName() != null && scheduleItem.getFacultyName().equalsIgnoreCase(selectedFaculty));
            boolean roomMatch = !filterByRoom || (scheduleItem.getRoom() != null && scheduleItem.getRoom().equalsIgnoreCase(selectedRoom));

            if (facultyMatch && roomMatch) {
                filteredSchedules.add(scheduleItem);
            }
        }
        scheduleTable.setItems(filteredSchedules);
        Label noSchedulesMessage = new Label("No schedules match the current filters.");
        noSchedulesMessage.getStyleClass().add("no-schedules-message");

        if (filteredSchedules.isEmpty()) {
            scheduleTable.setPlaceholder(noSchedulesMessage);
        } else {
            scheduleTable.setPlaceholder(null);
        }
    }

    private void handleImportRoomCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Room CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            // Show a loading alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Importing");
            alert.setHeaderText(null);
            alert.setContentText("Importing Rooms from CSV. Please wait...");
            alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            alert.show();

            Task<Void> importTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                        String line;
                        boolean firstRow = true;
                        while ((line = reader.readLine()) != null) {
                            if (firstRow) {
                                firstRow = false;
                                continue;
                            }

                            String[] columns = line.split(",");
                            if (columns.length == 2) {
                                String roomId = columns[0].trim();
                                String roomName = columns[1].trim();

                                String query = "INSERT INTO public.room (room_id, room_name) VALUES (?, ?)";
                                try (Connection conn = DBConnection.getConnection();
                                     PreparedStatement pstmt = conn.prepareStatement(query)) {
                                    pstmt.setInt(1, Integer.parseInt(roomId));
                                    pstmt.setString(2, roomName);
                                    pstmt.executeUpdate();
                                }
                            }
                        }
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    alert.close();
                    StageAndSceneUtils.showAlert("Success", "Room CSV imported successfully.", Alert.AlertType.INFORMATION);
                    Platform.runLater(() -> populateFilterRoomComboBox());
                }

                @Override
                protected void failed() {
                    alert.close();
                    logger.error("CSV import failed", getException());
                    StageAndSceneUtils.showAlert("Error", "Failed to import CSV file. Some rooms may exist already", Alert.AlertType.ERROR);
                }
            };

            new Thread(importTask).start();
        }
    }
}