package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminRoomAssignmentController {

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
    private StackPane root;
    @FXML
    private VBox vBox;
    @FXML
    private Label academicPeriod;
    @FXML
    private ComboBox<String> filterFacultyComboBox;
    @FXML
    private ComboBox<String> filterRoomComboBox;
    @FXML
    private Button addSchedule;

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
    private static final Logger logger = LoggerFactory.getLogger(AdminRoomAssignmentController.class);

    @FXML
    private void initialize() {
        vBox.toFront();
        scheduleContainer.setOpacity(0);
        initializeComboBoxes();
        scheduleTable.setEditable(false);

        Task<Void> task = getVoidTask();
        new Thread(task).start();

        // add more column cell if necessary
        var columns = new TableColumn[]{subCodeCell, subDescriptionCell, facultyNameCell, facultyIDCell, scheduleCell, roomCell, editCell};
        for (var col : columns) {
            col.setReorderable(false);
        }
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

        filterFacultyComboBox.setOnAction(_ -> filterSchedules());
        filterRoomComboBox.setOnAction(_ -> filterSchedules());

        updateCancelButton.setOnAction(_ -> handleCancelSchedule());
        createCancelButton.setOnAction(_ -> handleCancelSchedule());
        addSchedule.setOnAction(_ -> handleAddSchedule(borderPane, scheduleContainer));
    }

    private Task<Void> getVoidTask() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                loadSchedules();
                populateFacultyIDComboBox();
                populateFilterFacultyComboBox();
                populateFilterRoomComboBox();
                populateTimeComboBox();
                return null;
            }
        };
        task.setOnSucceeded(_ -> scheduleTable.refresh());
        task.setOnFailed(event -> logger.error("Failed to load schedules", event.getSource().getException()));
        return task;
    }

    private void loadSchedules() {
        String query = """
                    SELECT CONCAT(fac.faculty_number, ' - ', sub.description, ' (', fl.year_section, ')') AS faculty, fac.faculty_id, fac.firstname || ' ' || fac.lastname AS faculty_name, fac.faculty_number, fl.load_id, sub.subject_id, sub.subject_code, sub.description,
                           fl.year_section, sch.days, TO_CHAR(sch.start_time, 'HH:MI AM') AS start_time,
                           TO_CHAR(sch.end_time, 'HH:MI AM') AS end_time, r.room_name AS room, sub.units, sch.lecture_hour, sch.laboratory_hour
                    FROM schedule sch
                    JOIN faculty_load fl ON sch.faculty_load_id = fl.load_id
                    JOIN faculty fac ON fl.faculty_id = fac.faculty_id
                    JOIN subjects sub ON fl.subject_id = sub.subject_id
                    JOIN room r ON sch.room_id = r.room_id;
                """;

        try(Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

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

                editBtn.setOnAction(_ -> handleEditSchedule(schedule, borderPane, scheduleContainer));

                schedules.add(schedule);
                allSchedules.add(schedule);
            }

        } catch (SQLException ex) {
            logger.error("Error loading school events", ex);
        }
    }

    private void setWrappingHeaderCellFactory(TableColumn<Schedule, String> column) {

        AtomicBoolean isDarkTheme = new AtomicBoolean(root.getScene() != null && root.getScene().getRoot().getStyleClass().contains("dark-theme"));
        root.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                isDarkTheme.set(newScene.getRoot().getStyleClass().contains("dark-theme"));
                newScene.getRoot().getStyleClass().addListener((ListChangeListener<String>) change ->
                        isDarkTheme.set(change.getList().contains("dark-theme")));
            }
        });

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
                    setGraphic(label.getParent());  // StackPane as parent
                }
            }
        });
    }

    private void populateFacultyIDComboBox() {
        String query = """
                SELECT CONCAT(faculty_number, ' - ', description, ' (', year_section, ')') AS faculty
                FROM faculty_load
                JOIN faculty ON faculty.faculty_id = faculty_load.faculty_id
                JOIN subjects ON subjects.subject_id = faculty_load.subject_id
                WHERE faculty_load.semester = ?
                AND faculty_load.load_id NOT IN (SELECT faculty_load_id FROM schedule)
                ORDER BY faculty_number
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, SchoolYearAndSemester.determineCurrentSemester());
            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String faculty = rs.getString("faculty");
                    facultyIDComboBox.getItems().add(faculty);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to load faculty list", e);
        }
    }

    private void populateTimeComboBox() {
        String[] times = new String[28];
        LocalTime time = LocalTime.of(7, 30);
        for (int i = 0; i < times.length; i++) {
            times[i] = time.format(DateTimeFormatter.ofPattern("h:mm a"));
            time = time.plusMinutes(30);
            if (time.isAfter(LocalTime.of(22, 0))) {
                break;
            }
        }
        startTimeComboBox.getItems().addAll(times);
        endTimeComboBox.getItems().addAll(times);
    }

    private void populateFilterFacultyComboBox() {
        String query = "SELECT DISTINCT firstname || ' ' || lastname AS full_name FROM faculty";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String facultyName = rs.getString("full_name");
                filterFacultyComboBox.getItems().add(facultyName);
            }

        } catch (SQLException e) {
            logger.error("Failed to load faculty list", e);
        }
    }

    private void populateFilterRoomComboBox() {
        String query = "SELECT DISTINCT room_id, room_name FROM room ORDER BY room_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                filterRoomComboBox.getItems().add(rs.getString("room_name"));
                roomComboBox.getItems().add(rs.getString("room_name"));
            }
        } catch (SQLException e) {
            logger.error("Failed to load room list", e);
        }
    }

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

    private void handleAddSchedule(BorderPane borderPane, VBox scheduleContainer) {
        clearAllFields();
        borderPane.toFront();
        scheduleContainer.setDisable(false);
        scheduleContainer.setOpacity(1);
        showCreateButtonsContainer();
        facultyIDComboBox.setDisable(false);
        scheduleHeader.setText("Create Schedule");

        createButton.setOnAction(_ -> {
            int facultyLoadID = searchFacultyLoadID();
            if (facultyLoadID == -1) {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Please select a faculty load.");
                return;
            }

            String startTime = String.valueOf(startTimeComboBox.getValue());
            String endTime = String.valueOf(endTimeComboBox.getValue());

            int lectureHour, laboratoryHour;
            try {
                lectureHour = Integer.parseInt(lectureHourTextField.getText());
                laboratoryHour = Integer.parseInt(laboratoryHourTextField.getText());
            } catch (NumberFormatException e) {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Lecture or Laboratory hour must be a number.");
                return;
            }

            String room = roomComboBox.getValue();
            int roomID = 0;
            String days = getSelectedDays();

            boolean isScheduleFree = isScheduleFree(room, startTime, endTime, days, facultyLoadID);
            if (!isScheduleFree) {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Schedule is not free.");
                return;
            }

            String query = "INSERT INTO schedule (faculty_load_id, start_time, end_time, lecture_hour, laboratory_hour, days, room_id) " +
                    "VALUES (?, TO_TIMESTAMP(?, 'HH12:MI AM'), TO_TIMESTAMP(?, 'HH12:MI AM'), ?, ?, ?, ?)";

            String getRoomID = "SELECT room_id FROM room WHERE room_name = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query);
                 PreparedStatement roomStmt = conn.prepareStatement(getRoomID)) {

                roomStmt.setString(1, room);
                ResultSet rs = roomStmt.executeQuery();
                if (rs.next()) {
                    roomID = Integer.parseInt(rs.getString("room_id"));
                }

                stmt.setInt(1, facultyLoadID);
                stmt.setString(2, startTime);
                stmt.setString(3, endTime);
                stmt.setInt(4, lectureHour);
                stmt.setInt(5, laboratoryHour);
                stmt.setString(6, days);
                stmt.setInt(7, roomID);

                stmt.executeUpdate();

                // Refresh the schedule list
                schedules.clear();
                allSchedules.clear();
                loadSchedules();
                
                // Hide the schedule container after a successful addition
                handleCancelSchedule();

                populateFacultyIDComboBox(); // Refresh the faculty ID combo box after adding a schedule
                
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.INFORMATION), "Schedule added successfully!");

            } catch (SQLException e) {
                logger.error("Failed to add schedule", e);
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.ERROR), "Failed to add schedule.");
            }
        });
        scheduleTable.refresh();
    }

    private int searchFacultyLoadID() {
        String facultySplit = facultyIDComboBox.getValue();

        String queryForFacultyLoadID = """
                SELECT load_id FROM faculty_load
                JOIN faculty ON faculty_load.faculty_id = faculty.faculty_id
                JOIN subjects ON faculty_load.subject_id = subjects.subject_id
                WHERE faculty_number = ? AND description = ? AND year_section = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryForFacultyLoadID)) {

            String[] parts = facultySplit.split(" - | \\(|\\)");
            String facultyNumber = parts[0];
            String description = parts[1];
            String yearSection = parts[2];

            stmt.setString(1, facultyNumber.trim());
            stmt.setString(2, description.trim());
            stmt.setString(3, yearSection.trim());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("load_id");
            } else {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Faculty load not found.");
                return -1;
            }
        } catch (SQLException e) {
            StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.ERROR), "Error fetching faculty load.");
            return -1;
        }
    }

    private void handleEditSchedule(Schedule schedule, BorderPane borderPane, VBox scheduleContainer) {
        borderPane.toFront();
        scheduleContainer.setOpacity(1);
        scheduleContainer.setDisable(false);
        showUpdateButtonsContainer();
        facultyIDComboBox.setDisable(true);
        facultyIDComboBox.setValue(schedule.getFaculty());
        startTimeComboBox.setValue(schedule.getStartTime());
        endTimeComboBox.setValue(schedule.getEndTime());
        lectureHourTextField.setText(String.valueOf(schedule.getLectureHour()));
        laboratoryHourTextField.setText(String.valueOf(schedule.getLaboratoryHour()));
        roomComboBox.setValue(schedule.getRoom());

        String[] days = schedule.getDays().split("(?<=\\D)(?=M|T|W|Th|F|S|Su)");
        for (String day : days) {
            switch (day) {
                case "M": monCheckBox.setSelected(true); break;
                case "T": tueCheckBox.setSelected(true); break;
                case "W": wedCheckBox.setSelected(true); break;
                case "Th": thuCheckBox.setSelected(true); break;
                case "F": friCheckBox.setSelected(true); break;
                case "S": satCheckBox.setSelected(true); break;
                case "Su": sunCheckBox.setSelected(true); break;
            }
        }

        scheduleHeader.setText("Edit Schedule");

        updateButton.setOnAction(_ -> {
            // Collect updated data from UI components
            int UpdatedFacultyLoadID = searchFacultyLoadID();
            if (UpdatedFacultyLoadID == -1) {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Faculty load not found.");
                return;
            }
            int updatedLectureHour, updatedLaboratoryHour;
            try {
                updatedLectureHour = Integer.parseInt(lectureHourTextField.getText());
                updatedLaboratoryHour = Integer.parseInt(laboratoryHourTextField.getText());
            } catch (NumberFormatException e) {
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "Lecture or Laboratory hour must be a number.");
                return;
            }
            String updatedRoom = roomComboBox.getValue();
            String updatedStartTime = String.valueOf(startTimeComboBox.getValue());
            String updatedEndTime = String.valueOf(endTimeComboBox.getValue());
            String updatedDays = getSelectedDays();

            // Update the schedule object
            schedule.setLoadID(UpdatedFacultyLoadID);
            schedule.setLectureHour(updatedLectureHour);
            schedule.setLaboratoryHour(updatedLaboratoryHour);
            schedule.setRoom(updatedRoom);
            schedule.setStartTime(updatedStartTime);
            schedule.setEndTime(updatedEndTime);
            schedule.setDays(updatedDays);

            // Update the database
            String updateQuery = "UPDATE schedule SET start_time = TO_TIMESTAMP(?, 'HH12:MI AM'), end_time = TO_TIMESTAMP(?, 'HH12:MI AM'), lecture_hour = ?, laboratory_hour = ?, days = ?, room_id = ? WHERE faculty_load_id = ?";

            String getRoomID = "SELECT room_id FROM room WHERE room_name = ?";
            int roomID = 0;
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateQuery);
                 PreparedStatement roomStmt = conn.prepareStatement(getRoomID)) {

                roomStmt.setString(1, updatedRoom);
                ResultSet rs = roomStmt.executeQuery();
                if (rs.next()) {
                    roomID = Integer.parseInt(rs.getString("room_id"));
                }
                stmt.setString(1, updatedStartTime);
                stmt.setString(2, updatedEndTime);
                stmt.setInt(3, updatedLectureHour);
                stmt.setInt(4, updatedLaboratoryHour);
                stmt.setString(5, updatedDays);
                stmt.setInt(6, roomID);
                stmt.setInt(7, UpdatedFacultyLoadID);
                stmt.executeUpdate();
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.INFORMATION), "Schedule updated successfully!");
            } catch (SQLException e) {
                logger.error("Failed to update schedule in the database", e);
                StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.ERROR), "Failed to update schedule in the database.");
            }

            // Refresh the table view or perform additional actions as needed
            scheduleTable.refresh();
            handleCancelSchedule(); // Return to the previous view
            reloadFXML();
        });
        deleteButton.setOnAction(_ -> handleDeleteSchedule(schedule));
    }

    private void handleDeleteSchedule(Schedule schedule) {
        int facultyLoadID = schedule.getLoadID();

        try {
            String deleteQuery = "DELETE FROM schedule WHERE faculty_load_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setInt(1, facultyLoadID);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Clear and reload all schedules
                    schedules.clear();
                    allSchedules.clear();
                    loadSchedules();
                    
                    // Hide the schedule container
                    handleCancelSchedule();
                    
                    // Refresh the table
                    scheduleTable.refresh();
                    
                    // Show a success message
                    StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.INFORMATION), "Schedule deleted successfully!");
                    
                    // Reload the FXML if needed
                    reloadFXML();
                } else {
                    StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.WARNING), "No schedule was deleted. The record may have been removed already.");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to delete schedule", e);
            StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.ERROR), "Failed to delete schedule.");
        }
    }

    private boolean isScheduleFree(String room, String proposedStart, String proposedEnd, String proposedDays, int facultyLoadId) {
        String query = """
        SELECT s.days
        FROM schedule s
        JOIN room r ON s.room_id = r.room_id
        WHERE (
            TO_TIMESTAMP(?, 'HH12:MI AM')::time < s.end_time
            AND TO_TIMESTAMP(?, 'HH12:MI AM')::time > s.start_time
            AND (r.room_name = ? OR s.faculty_load_id = ?)
            AND s.faculty_load_id != ?
        )
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, proposedStart);
            statement.setString(2, proposedEnd);
            statement.setString(3, room);
            statement.setInt(4, facultyLoadId);
            statement.setInt(5, facultyLoadId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String scheduledDays = rs.getString("days");
                if (daysOverlap(scheduledDays, proposedDays)) {
                    return false; // conflict found
                }
            }
            return true; // no conflict
        } catch (SQLException e) {
            logger.error("Failed to check schedule conflicts", e);
            return false;
        }
    }

    private boolean daysOverlap(String existingDays, String proposedDays) {
        // example: existing = "TTh", proposed = "Th"
        // check if any of the proposed substrings exist in the scheduled days
        List<String> dayTokens = Arrays.asList("M", "T", "W", "Th", "F", "Su", "S"); // adjust as needed

        for (String token : dayTokens) {
            if (existingDays.contains(token) && proposedDays.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private void reloadFXML() {
        try {
            // Get the current stage
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            
            // Load the same FXML again
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/pupsis_main_dashboard/FXML/Admin/AdminRoomAssignment.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // Set the scene with the reloaded FXML
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to reload FXML", e);
            StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.ERROR), "Failed to reload the interface.");
        }
    }

    private void handleCancelSchedule() {
        vBox.toFront();
        scheduleContainer.setDisable(true);
        scheduleContainer.setOpacity(0);
        clearAllFields();
        showCreateButtonsContainer();
    }

    private void showCreateButtonsContainer() {
        createScheduleButtonsContainer.toFront();
        createScheduleButtonsContainer.setOpacity(1);
        createScheduleButtonsContainer.setDisable(false);
        updateScheduleButtonsContainer.toFront();
        updateScheduleButtonsContainer.setOpacity(0);
        updateScheduleButtonsContainer.setDisable(true);
    }

    private void showUpdateButtonsContainer() {
        updateScheduleButtonsContainer.toFront();
        updateScheduleButtonsContainer.setOpacity(1);
        updateScheduleButtonsContainer.setDisable(false);
        createScheduleButtonsContainer.toFront();
        createScheduleButtonsContainer.setOpacity(0);
        createScheduleButtonsContainer.setDisable(true);
    }

    private void initializeComboBoxes() {
        ObservableList<String> facultyOptions = FXCollections.observableArrayList("All Faculty");
        ObservableList<String> roomOptions = FXCollections.observableArrayList("All Room");

        for (Schedule schedule : allSchedules) {
            if (!facultyOptions.contains(schedule.getFacultyName())) {
                facultyOptions.add(schedule.getFacultyName());
            }
            if (!roomOptions.contains(schedule.getRoom())) {
                roomOptions.add(schedule.getRoom());
            }
        }

        filterFacultyComboBox.setItems(facultyOptions);
        filterRoomComboBox.setItems(roomOptions);

        filterFacultyComboBox.getSelectionModel().select("All Faculty");
        filterRoomComboBox.getSelectionModel().select("All Room");
    }

    private void filterSchedules() {
        String selectedFaculty = filterFacultyComboBox.getSelectionModel().getSelectedItem();
        String selectedRoom = filterRoomComboBox.getSelectionModel().getSelectedItem();

        boolean allFaculties = selectedFaculty == null || selectedFaculty.equals("All Faculty");
        boolean allRooms = selectedRoom == null || selectedRoom.equals("All Room");

        schedules.clear();

        for (Schedule schedule : allSchedules) {
            boolean matchesFaculty = allFaculties || schedule.getFacultyName().toLowerCase().contains(selectedFaculty.toLowerCase());
            boolean matchesRoom = allRooms || schedule.getRoom().toLowerCase().contains(selectedRoom.toLowerCase());

            if (matchesFaculty && matchesRoom) {
                schedules.add(schedule);
            }
        }
        if (schedules.isEmpty()) {
            scheduleTable.setPlaceholder(new Text("No schedules found"));
        } else {
            scheduleTable.setPlaceholder(null);
        }
    }

    private void clearAllFields() {
        facultyIDComboBox.setValue(null);
        lectureHourTextField.clear();
        laboratoryHourTextField.clear();
        roomComboBox.setValue(null);
        startTimeComboBox.setValue(null);
        endTimeComboBox.setValue(null);
        monCheckBox.setSelected(false);
        tueCheckBox.setSelected(false);
        wedCheckBox.setSelected(false);
        thuCheckBox.setSelected(false);
        friCheckBox.setSelected(false);
        satCheckBox.setSelected(false);
        sunCheckBox.setSelected(false);
    }
}