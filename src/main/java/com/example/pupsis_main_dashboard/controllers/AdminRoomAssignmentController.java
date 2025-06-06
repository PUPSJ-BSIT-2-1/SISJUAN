package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester;
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
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        }

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

        for (TableColumn<Schedule, ?> col : Arrays.asList(subCodeCell, subDescriptionCell, facultyNameCell, facultyIDCell, scheduleCell, roomCell)) {
            setWrappingHeaderCellFactory(col); 
        }

        scheduleTable.setRowFactory(_ -> {
            TableRow<Schedule> row = new TableRow<>();
            row.setPrefHeight(65);
            return row;
        });

        filterFacultyComboBox.setOnAction(_ -> applyFilters());
        filterRoomComboBox.setOnAction(_ -> applyFilters());

        updateCancelButton.setOnAction(_ -> handleCancelSchedule());
        createCancelButton.setOnAction(_ -> handleCancelSchedule());
        addSchedule.setOnAction(_ -> displayCreateScheduleForm());

        deleteButton.setOnAction(event -> {
            Schedule selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
            if (selectedSchedule != null) {
                handleDeleteSchedule(selectedSchedule);
            } else {
                StageAndSceneUtils.showAlert("Selection Error", "Please select a schedule from the table to delete.", Alert.AlertType.WARNING);
            }
        });
    }

    private void displayCreateScheduleForm() {
        borderPane.toFront(); 
        scheduleContainer.setDisable(false);
        scheduleContainer.setOpacity(1);
        clearAllFields();
        populateFacultyIDComboBox(); 
        populateTimeComboBox();     
        populateFilterRoomComboBox(); 
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
                ys.year_section
            FROM public.schedule s
            JOIN public.faculty_load fl ON s.faculty_load_id = fl.load_id
            JOIN public.subjects sub ON fl.subject_id = sub.subject_id
            JOIN public.faculty f ON fl.faculty_id = f.faculty_id
            JOIN public.room r ON s.room_id = r.room_id
            JOIN public.year_section ys ON fl.section_id = ys.section_id
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
                    int loadId = rs.getInt("load_id");
                    // String facultyValue = rs.getString("faculty_name"); // This was for the combo box, not directly for schedule display
                    String subjectCode = rs.getString("subject_code"); // Corrected from subjectID to subjectCode for clarity
                    String facultyNumber = rs.getString("faculty_number");
                    String subDesc = rs.getString("subject_description");
                    String facultyName = rs.getString("faculty_name");
                    // String facultyID = rs.getString("faculty_number"); // faculty_id is likely preferred if available from faculty table
                    String yearSection = rs.getString("year_section");
                    String days = rs.getString("days");
                    Time startTimeSql = rs.getTime("start_time");
                    Time endTimeSql = rs.getTime("end_time");
                    String startTime = (startTimeSql != null) ? new SimpleDateFormat("hh:mm a").format(startTimeSql) : "";
                    String endTime = (endTimeSql != null) ? new SimpleDateFormat("hh:mm a").format(endTimeSql) : "";
                    String room = rs.getString("room_name");
                    String unitsStr = rs.getString("units");
                    int lectureHour = rs.getInt("lecture_hour");
                    int laboratoryHour = rs.getInt("laboratory_hour");

                    // Constructing facultyValue for display consistency if needed, or use facultyName directly
                    String facultyDisplayValue = facultyName + " (" + facultyNumber + ")";

                    Schedule schedule = new Schedule(loadId, facultyDisplayValue, subjectCode, facultyNumber, subjectCode, subDesc, facultyName, facultyNumber, yearSection, days, startTime, endTime, room, unitsStr, lectureHour, laboratoryHour, editButton);
                    editButton.setOnAction(_ -> handleEditSchedule(schedule, borderPane, scheduleContainer));
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
    private void setWrappingHeaderCellFactory(TableColumn<Schedule, ?> column) {
        Label headerLabel = new Label(column.getText()); 
        headerLabel.setWrapText(true);
        headerLabel.setStyle("-fx-padding: 2px; -fx-text-alignment: center; -fx-alignment: center;");
        headerLabel.setMaxWidth(Double.MAX_VALUE);

        StackPane stackPane = new StackPane(headerLabel);
        stackPane.setPrefHeight(Control.USE_COMPUTED_SIZE);
        StackPane.setAlignment(headerLabel, Pos.CENTER);

        column.setGraphic(stackPane);
        column.setText(null); 
    }

    // This method populates the faculty ID combo box with faculty load information for unscheduled loads.
    private void populateFacultyIDComboBox() {
        ObservableList<String> facultyLoadStrings = FXCollections.observableArrayList();
        String currentSemesterName = SchoolYearAndSemester.determineCurrentSemester();
        int semesterId = SchoolYearAndSemester.getSemesterId(currentSemesterName);
        int academicYearId = SchoolYearAndSemester.getCurrentAcademicYearId();

        facultyIDComboBox.getItems().clear();
        String query = """
                SELECT CONCAT(fac.faculty_number, ' - ', sub.description, ' (', ys.year_section, ')') AS faculty_load_display
                FROM public.faculty_load fl
                JOIN public.faculty fac ON fl.faculty_id = fac.faculty_id
                JOIN public.subjects sub ON fl.subject_id = sub.subject_id
                JOIN public.year_section ys ON fl.section_id = ys.section_id
                LEFT JOIN public.schedule sch ON fl.load_id = sch.faculty_load_id
                WHERE sch.schedule_id IS NULL AND fl.semester_id = ? AND fl.academic_year_id = ?
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
        facultyNames.add("All Faculty"); // Add option for no filter
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
        filterRoomNames.add("All Rooms"); // Add option for no filter
        ObservableList<String> selectionRoomNames = FXCollections.observableArrayList(); 

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String roomName = rs.getString("room_name");
                filterRoomNames.add(roomName);
                selectionRoomNames.add(roomName);
            }
            filterRoomComboBox.setItems(filterRoomNames);
            roomComboBox.setItems(selectionRoomNames); 
            if (!filterRoomNames.isEmpty()) {
                filterRoomComboBox.getSelectionModel().selectFirst(); // Default to "All Rooms"
            }
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

        String insertScheduleQuery = "INSERT INTO public.schedule (faculty_load_id, room_id, start_time, end_time, days, lecture_hour, laboratory_hour) VALUES (?, ?, TO_TIMESTAMP(?, 'HH12:MI AM'), TO_TIMESTAMP(?, 'HH12:MI AM'), ?, ?, ?)";
        String getRoomIDQuery = "SELECT room_id FROM public.room WHERE room_name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement roomStmt = conn.prepareStatement(getRoomIDQuery)) {
            roomStmt.setString(1, roomName);
            ResultSet rsRoom = roomStmt.executeQuery();
            int roomID = -1;
            if (rsRoom.next()) {
                roomID = rsRoom.getInt("room_id");
            } else {
                StageAndSceneUtils.showAlert("Data Error", "Selected room not found.", Alert.AlertType.ERROR);
                return;
            }

            try (PreparedStatement scheduleStmt = conn.prepareStatement(insertScheduleQuery)) {
                scheduleStmt.setInt(1, facultyLoadID);
                scheduleStmt.setInt(2, roomID);
                scheduleStmt.setString(3, startTime);
                scheduleStmt.setString(4, endTime);
                scheduleStmt.setString(5, days);
                scheduleStmt.setInt(6, lectureHourInt);
                scheduleStmt.setInt(7, laboratoryHourInt);
                scheduleStmt.executeUpdate();

                schedules.clear();
                allSchedules.clear();
                loadSchedules(); 
                scheduleTable.setItems(schedules);
                populateFacultyIDComboBox(); 
                StageAndSceneUtils.showAlert("Success", "Schedule added successfully!", Alert.AlertType.INFORMATION);
                handleCancelSchedule(); // Close the form
            }
        } catch (SQLException e) {
            logger.error("Failed to add schedule", e);
            StageAndSceneUtils.showAlert("Database Error", "Failed to add schedule. It might conflict with an existing schedule or violate database constraints.", Alert.AlertType.ERROR);
        }
        scheduleTable.refresh();
    }

    // This method searches for the faculty load ID based on the selected faculty in the combo box.
    private int searchFacultyLoadID(String facultyComboBoxValue) {
        if (facultyComboBoxValue == null || facultyComboBoxValue.isEmpty()) {
            logger.warn("Faculty combo box value is null or empty in searchFacultyLoadID.");
            return -1; 
        }

        String[] mainParts = facultyComboBoxValue.split(" - ", 2);
        if (mainParts.length < 2) {
            logger.warn("Invalid format for facultyComboBoxValue (main split): " + facultyComboBoxValue);
            return -1;
        }
        String facultyNumber = mainParts[0].trim();
        
        String subjectAndSectionPart = mainParts[1];
        int lastOpenParen = subjectAndSectionPart.lastIndexOf(" (");
        if (lastOpenParen == -1 || !subjectAndSectionPart.endsWith(")")) {
            logger.warn("Invalid format for subject and section part (paren split): " + subjectAndSectionPart);
            return -1;
        }

        String subjectDescription = subjectAndSectionPart.substring(0, lastOpenParen).trim();
        String yearAndSectionFull = subjectAndSectionPart.substring(lastOpenParen + 2, subjectAndSectionPart.length() - 1).trim();
        
        String[] yearSectionParts = yearAndSectionFull.split(" - ", 2);
        if (yearSectionParts.length < 2) {
            logger.warn("Invalid format for year and section (year-section split): " + yearAndSectionFull);
            return -1;
        }
        String yearLevelName = yearSectionParts[0].trim();
        String sectionName = yearSectionParts[1].trim();

        String query = """
            SELECT fl.load_id 
            FROM public.faculty_load fl
            JOIN public.faculty fac ON fl.faculty_id = fac.faculty_id
            JOIN public.subjects sub ON fl.subject_id = sub.subject_id
            JOIN public.year_section ys ON fl.section_id = ys.section_id
            WHERE fac.faculty_number = ? AND sub.description = ? AND ys.year_section = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, facultyNumber);
            pstmt.setString(2, subjectDescription);
            pstmt.setString(3, yearAndSectionFull);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("load_id");
            } else {
                logger.warn("Faculty load not found in DB for: FN='{}', Desc='{}', YS='{}'", facultyNumber, subjectDescription, yearAndSectionFull);
                return -1; 
            }
        } catch (SQLException e) {
            logger.error("Error searching faculty load ID for: " + facultyComboBoxValue, e);
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

        facultyIDComboBox.setValue(schedule.getFaculty()); 
        lectureHourTextField.setText(String.valueOf(schedule.getLectureHour()));
        laboratoryHourTextField.setText(String.valueOf(schedule.getLaboratoryHour()));
        roomComboBox.setValue(schedule.getRoom());
        startTimeComboBox.setValue(schedule.getStartTime());
        endTimeComboBox.setValue(schedule.getEndTime());

        String days = schedule.getDays();
        monCheckBox.setSelected(days.contains("M"));
        tueCheckBox.setSelected(days.contains("T") && !days.contains("Th"));
        wedCheckBox.setSelected(days.contains("W"));
        thuCheckBox.setSelected(days.contains("Th"));
        friCheckBox.setSelected(days.contains("F"));
        satCheckBox.setSelected(days.contains("S") && !days.contains("Su"));
        sunCheckBox.setSelected(days.contains("Su"));

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

            String updateQuery = "UPDATE public.schedule SET room_id = ?, start_time = TO_TIMESTAMP(?, 'HH12:MI AM'), end_time = TO_TIMESTAMP(?, 'HH12:MI AM'), days = ?, lecture_hour = ?, laboratory_hour = ? WHERE faculty_load_id = ?";
            String getRoomIDQuery = "SELECT room_id FROM public.room WHERE room_name = ?";
            int roomIDToUpdate = -1;

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement roomStmt = conn.prepareStatement(getRoomIDQuery)) {
                roomStmt.setString(1, updatedRoomName);
                ResultSet rsRoom = roomStmt.executeQuery();
                if (rsRoom.next()) {
                    roomIDToUpdate = rsRoom.getInt("room_id");
                } else {
                    StageAndSceneUtils.showAlert("Data Error", "Selected room not found.", Alert.AlertType.ERROR);
                    return;
                }

                try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateQuery)) {
                    pstmtUpdate.setInt(1, roomIDToUpdate);
                    pstmtUpdate.setString(2, updatedStartTime);
                    pstmtUpdate.setString(3, updatedEndTime);
                    pstmtUpdate.setString(4, updatedDays);
                    pstmtUpdate.setInt(5, updatedLectureHour);
                    pstmtUpdate.setInt(6, updatedLaboratoryHour);
                    pstmtUpdate.setInt(7, facultyLoadIDToUpdate);
                    pstmtUpdate.executeUpdate();
                    StageAndSceneUtils.showAlert("Success", "Schedule updated successfully!", Alert.AlertType.INFORMATION);

                    schedules.clear();
                    allSchedules.clear();
                    loadSchedules();
                    scheduleTable.setItems(schedules);
                    handleCancelSchedule(); 
                }
            } catch (SQLException e) {
                logger.error("Failed to update schedule in the database", e);
                StageAndSceneUtils.showAlert("Database Error", "Failed to update schedule. It might conflict or violate constraints.", Alert.AlertType.ERROR);
            }
            scheduleTable.refresh();

            handleCancelSchedule(); // Return to the previous view
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
                    scheduleTable.refresh();

                    handleCancelSchedule(); 

                    populateFacultyIDComboBox();
                    
                    // Show a success message
                    StageAndSceneUtils.showAlert(String.valueOf(Alert.AlertType.INFORMATION), "Schedule deleted successfully!");
                } else {
                    StageAndSceneUtils.showAlert("Deletion Warning", "No schedule was deleted. The record may have been removed by another user or an error occurred.", Alert.AlertType.WARNING);
                }
            } catch (SQLException e) {
                logger.error("Failed to delete schedule with faculty_load_id: " + facultyLoadID, e);
                StageAndSceneUtils.showAlert("Database Error", "Failed to delete schedule due to a database error.", Alert.AlertType.ERROR);
            }
        }
    }

    // This method checks if the proposed schedule is free in the specified room.
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

    // This method checks if the existing days overlap with the proposed days.
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
        if (filteredSchedules.isEmpty()) {
            scheduleTable.setPlaceholder(new Text("No schedules match the current filters."));
        } else {
            scheduleTable.setPlaceholder(null);
        }
    }
}