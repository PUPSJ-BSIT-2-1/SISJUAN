package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent; 
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentEnrollmentController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(StudentEnrollmentController.class);
    private static final int MAX_UNITS = 24;

    @FXML private VBox subjectListContainer;
    @FXML private VBox enrolledSubjectsDisplayContainer; 
    @FXML private Button selectAllButton;
    @FXML private Button enrollButton;
    @FXML private Label currentYearLevelDisplayLabel;
    @FXML private Label currentSemesterDisplayLabel;
    @FXML private ProgressIndicator loadingIndicator; 
    @FXML private Label unitCounterLabel; // Added for unit counting

    private StudentEnrollmentContext studentEnrollmentContext; 
    private List<SubjectData> availableSubjects;
    private List<CheckBox> subjectCheckboxes = new ArrayList<>();
    private Map<CheckBox, SubjectData> checkboxSubjectMap = new HashMap<>();
    private Map<CheckBox, ComboBox<String>> subjectScheduleMap = new HashMap<>();
    private int currentSelectedUnits = 0;

    private static final List<String> TIME_SLOTS = Arrays.asList(
            "Mon/Wed 9:00-10:30 AM",
            "Tue/Thu 1:00-2:30 PM",
            "Fri 9:00-12:00 PM"
            // Add more actual time slots
    );

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (selectAllButton != null) {
            selectAllButton.setOnAction(this::handleSelectAll);
        }
        if (enrollButton != null) {
            enrollButton.setOnAction(this::handleEnrollment);
            enrollButton.setDisable(true); // Initially disable until subjects are selected
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        if (unitCounterLabel == null) {
            logger.warn("unitCounterLabel is not injected. Unit counting UI will not be updated.");
        } else {
            unitCounterLabel.setText("Selected Units: 0/" + MAX_UNITS);
        }
        refreshEnrollmentView(); // Initial data load and view setup
        updateDynamicUnitCountAndEnrollButtonState(); // Initial state for unit counter
    }

    @FXML
    private void handleEnrollment(ActionEvent event) { 
        if (currentSelectedUnits > MAX_UNITS) {
            showAlert("Unit Limit Exceeded", "You cannot enroll in more than " + MAX_UNITS + " units. You have selected " + currentSelectedUnits + " units.", Alert.AlertType.WARNING);
            // enrollButton might already be disabled by updateDynamicUnitCountAndEnrollButtonState, but ensure it's re-enabled if logic changes
            // enrollButton.setDisable(false); // This might not be needed if updateDynamicUnitCountAndEnrollButtonState handles it
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            return;
        }

        List<EnrollmentData> selectedSubjects = subjectCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> {
                    SubjectData subjectData = checkboxSubjectMap.get(cb);
                    ComboBox<String> scheduleComboBox = subjectScheduleMap.get(cb);
                    String selectedScheduleDisplayString = scheduleComboBox.getValue(); // This is the display string like "MWF 9:00-10:00 AM - Room 101"

                    if (selectedScheduleDisplayString == null || selectedScheduleDisplayString.equals("No schedules available") || selectedScheduleDisplayString.equals("No specific schedules listed") || selectedScheduleDisplayString.equals("Schedule TBD")) {
                        showAlert("Invalid Selection", "Please select a valid schedule for " + subjectData.subjectCode() + ".", Alert.AlertType.WARNING);
                        return null;
                    }
                    // The EnrollmentData's 'schedule' field is used as faculty_load_id in the INSERT query.
                    // subjectData.offeringId() holds the faculty_load_id for this subject offering.
                    return new EnrollmentData(subjectData.subjectId(), subjectData.offeringId());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (selectedSubjects.isEmpty()) {
            showAlert("No Selection", "Please select at least one subject to enroll.","warning");
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            enrollButton.setDisable(false); 
            return;
        }

        enrollButton.setDisable(true);
        selectAllButton.setDisable(true);
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        Task<Boolean> enrollmentTask = new Task<>() { 
            @Override
            protected Boolean call() throws Exception {
                StudentEnrollmentContext currentContext = fetchStudentEnrollmentContext(); // Fetch context within the task
                logger.debug("handleEnrollment Task: Fetched context: {}", currentContext);

                if (currentContext == null || currentContext.studentId() <= 0) { // Use currentContext and check studentId validity
                    logger.error("handleEnrollment Task: Student context or ID not found for enrollment.");
                    throw new SQLException("Student context or ID not found for enrollment.");
                }
                int studentDbId = currentContext.studentId(); // Use studentId from currentContext
                String studentCurrentYearSection = currentContext.studentYearSection(); // Use yearSection from currentContext

                if (studentCurrentYearSection == null || studentCurrentYearSection.trim().isEmpty()) {
                     logger.error("handleEnrollment Task: Could not retrieve student's current year section from context for enrollment.");
                     throw new SQLException("Could not retrieve student's current year section from context for enrollment.");
                }

                String academicYear = getCurrentAcademicYear();
                // Ensure currentSemesterForDB is derived correctly, possibly from currentContext if UI label is not reliable here
                String currentSemesterForDB = currentContext.semesterString(); 
                if (currentSemesterForDB == null || currentSemesterForDB.equals("N/A") || currentSemesterForDB.trim().isEmpty()) {
                     logger.error("handleEnrollment Task: Could not determine current semester for enrollment from context.");
                     throw new SQLException("Could not determine current semester for enrollment from context.");
                }

                Connection connection = null;
                try {
                    connection = DBConnection.getConnection();
                    connection.setAutoCommit(false);

                    // Get the next load_id for student_load. This assumes load_id is an auto-increment or needs manual sequence handling.
                    // For simplicity, let's assume it's manually handled or you have a sequence. Here, we'll find max + 1.
                    // THIS IS NOT ROBUST FOR CONCURRENT ENVIRONMENTS. A SEQUENCE IS PREFERRED.
                    int nextLoadId = 0;
                    try (Statement stmtMaxId = connection.createStatement();
                         ResultSet rsMaxId = stmtMaxId.executeQuery("SELECT MAX(load_id) FROM student_load")) {
                        if (rsMaxId.next()) {
                            nextLoadId = rsMaxId.getInt(1) + 1;
                        }
                        if (nextLoadId == 0) nextLoadId = 1; // First entry
                    }

                    // student_load: student_pk_id, subject_id, semester_id, load_id (PK), academic_year_id, section_id, faculty_load (FK to faculty_load.load_id)
                    // Note: year_section string is not directly stored in student_load, section_id is used.
                    // academic_year string is not directly stored, academic_year_id is used.
                    String insertSql = "INSERT INTO student_load (student_pk_id, subject_id, semester_id, load_id, academic_year_id, section_id, faculty_load) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                        for (EnrollmentData enrollment : selectedSubjects) {
                            stmt.setInt(1, studentDbId); // student_pk_id from students table
                            stmt.setInt(2, Integer.parseInt(enrollment.subjectId())); // subject_id from subjects table
                            stmt.setInt(3, currentContext.semesterId()); // semester_id from context
                            stmt.setInt(4, nextLoadId++); // PK for student_load
                            stmt.setInt(5, currentContext.academicYearId()); // academic_year_id from context
                            stmt.setInt(6, currentContext.sectionId()); // section_id from context (represents student's year_section)
                            stmt.setInt(7, Integer.parseInt(enrollment.schedule())); // faculty_load (offeringId)

                            logger.debug("Adding to batch: student_pk_id={}, subject_id={}, semester_id={}, load_id={}, academic_year_id={}, section_id={}, faculty_load={}",
                                    studentDbId, enrollment.subjectId(), currentContext.semesterId(), nextLoadId - 1, currentContext.academicYearId(), currentContext.sectionId(), enrollment.schedule());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                    connection.commit();
                    return true;
                } catch (SQLException e) {
                    // Log error properly
                    logger.error("Enrollment failed in task: ", e); 
                    if (connection != null) {
                        try {
                            connection.rollback();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                    throw e; // Re-throw to be caught by task's exception handler
                }
            }
        };
        enrollmentTask.setOnSucceeded(workerStateEvent -> {
            if (enrollmentTask.getValue()) { 
                showAlert("Enrollment Successful", "Subjects enrolled successfully.","info");
                refreshEnrollmentView(); // Refresh data and view after successful enrollment
            } else {
                // This case might not be hit if exceptions are thrown for failures
                showAlert("Enrollment Failed", "Failed to enroll subjects. Please check details.","error");
            }
            clearEnrollmentUIState(); // Clears checkbox selections etc.
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            enrollButton.setDisable(false);
            selectAllButton.setDisable(false);
        });

        enrollmentTask.setOnFailed(workerStateEvent -> { 
            Throwable exception = enrollmentTask.getException(); 
            logger.error("Enrollment failed in task: ", exception); 
            showAlert("Enrollment Failed", "An error occurred during enrollment: " + exception.getMessage(), Alert.AlertType.ERROR);
            if (loadingIndicator != null) loadingIndicator.setVisible(false);
            enrollButton.setDisable(false);
            selectAllButton.setDisable(false);
        });

        new Thread(enrollmentTask).start(); 
    }

    private void clearEnrollmentUIState() {
        for (CheckBox cb : subjectCheckboxes) {
            cb.setSelected(false);
        }
        for (ComboBox<String> cbBox : subjectScheduleMap.values()) {
            cbBox.getSelectionModel().clearSelection();
            cbBox.setPromptText("Select Schedule"); 
        }
        updateSelectAllButtonState(); 
    }

    private void updateSelectAllButtonState() {
        if (selectAllButton != null) {
            List<CheckBox> enabledCheckboxes = subjectCheckboxes.stream()
                                                              .filter(cb -> !cb.isDisabled())
                                                              .collect(Collectors.toList());

            if (enabledCheckboxes.isEmpty()) {
                selectAllButton.setText("Select All");
                selectAllButton.setDisable(true);
            } else {
                boolean allEnabledSelected = enabledCheckboxes.stream().allMatch(CheckBox::isSelected);
                selectAllButton.setText(allEnabledSelected ? "Deselect All" : "Select All");
                selectAllButton.setDisable(false);
            }
        }
    }

    @FXML
    private void handleSelectAll(ActionEvent event) {
        List<CheckBox> enabledCheckboxes = subjectCheckboxes.stream()
                                                          .filter(cb -> !cb.isDisabled())
                                                          .collect(Collectors.toList());

        if (enabledCheckboxes.isEmpty()) {
            return; // No enabled checkboxes to act upon
        }

        // Determine target state based on whether all *enabled* checkboxes are currently selected
        boolean allCurrentlyEnabledSelected = enabledCheckboxes.stream().allMatch(CheckBox::isSelected);
        boolean targetState = !allCurrentlyEnabledSelected;

        for (CheckBox cb : enabledCheckboxes) {
            cb.setSelected(targetState);
        }
        updateSelectAllButtonState(); // Update button text and state
        updateEnrollButtonState();    // Update enroll button based on new selection
    }

    private void updateEnrollButtonState() {
        if (enrollButton != null) {
            List<CheckBox> enabledCheckboxes = subjectCheckboxes.stream()
                                                              .filter(cb -> !cb.isDisabled())
                                                              .collect(Collectors.toList());

            boolean anyEnabledSelected = enabledCheckboxes.stream().anyMatch(CheckBox::isSelected);
            enrollButton.setDisable(!anyEnabledSelected);
        }
    }

    private void populateSubjectListUI(EnrollmentSubjectLists subjectLists, StudentEnrollmentContext context) {
        logger.debug("populateSubjectListUI: Populating UI with {} enrolled and {} available subjects.", 
            subjectLists.enrolledSubjects().size(), subjectLists.availableSubjects().size());
        subjectCheckboxes.clear();
        checkboxSubjectMap.clear();
        subjectScheduleMap.clear();

        this.availableSubjects = subjectLists.availableSubjects; 

        if (enrolledSubjectsDisplayContainer != null && subjectLists.enrolledSubjects != null && !subjectLists.enrolledSubjects.isEmpty()) {
            // Optional: Add headers for enrolled subjects section for consistency
            HBox enrolledHeaderBox = new HBox(10);
            enrolledHeaderBox.setPadding(new Insets(0, 0, 5, 0)); 
            Label enrolledSpacer = new Label(); // Spacer for checkbox column alignment
            enrolledSpacer.setPrefWidth(40); // Match checkbox area width
            enrolledSpacer.getStyleClass().add("header-spacer"); 
            Label enrolledCodeHeader = new Label("Subject Code");
            enrolledCodeHeader.setPrefWidth(200);
            enrolledCodeHeader.getStyleClass().add("section-header");
            Label enrolledDescHeader = new Label("Description");
            enrolledDescHeader.setPrefWidth(300);
            enrolledDescHeader.getStyleClass().add("section-header");
            Label enrolledSchedHeader = new Label("Enrolled Schedule");
            enrolledSchedHeader.setPrefWidth(200); // Adjust as needed
            enrolledSchedHeader.getStyleClass().add("section-header");
            enrolledHeaderBox.getChildren().addAll(enrolledSpacer, enrolledCodeHeader, enrolledDescHeader, enrolledSchedHeader);
            enrolledSubjectsDisplayContainer.getChildren().add(enrolledHeaderBox);
            enrolledSubjectsDisplayContainer.getChildren().add(new Separator());

            for (SubjectData subject : subjectLists.enrolledSubjects()) {
                HBox subjectBox = new HBox(10);
                subjectBox.setPadding(new Insets(5,0,5,0)); // Consistent padding
                subjectBox.setAlignment(Pos.CENTER_LEFT);
                subjectBox.getStyleClass().add("subject-row"); // Use similar styling if desired

                Label spacerLabel = new Label(); // Spacer to align with checkbox column
                spacerLabel.setPrefWidth(40); // Match checkbox area width
                spacerLabel.getStyleClass().add("cell-spacer");

                Label subjectInfoLabel = new Label(String.format("%s - %s (%d units)",
                        subject.subjectCode(), subject.description(), subject.units()));
                subjectInfoLabel.setPrefWidth(510); // Combined width for code and description for now
                subjectInfoLabel.setWrapText(true);
                subjectInfoLabel.getStyleClass().add("enrolled-subject-info"); // Specific style for enrolled info

                Label scheduleLabel = new Label(subject.enrolledSchedule() != null ? subject.enrolledSchedule() : "N/A");
                scheduleLabel.setPrefWidth(200); // Match schedule column width
                scheduleLabel.setWrapText(true);
                scheduleLabel.getStyleClass().add("enrolled-subject-schedule"); // Specific style for enrolled schedule

                // For a more column-like appearance for subjectInfoLabel:
                Label codeLabel = new Label(subject.subjectCode());
                codeLabel.setPrefWidth(200);
                codeLabel.getStyleClass().add("subject-code"); // Reuse existing style if applicable
                
                Label descLabel = new Label(subject.description() + " (" + subject.units() + " units)");
                descLabel.setPrefWidth(300);
                descLabel.setWrapText(true);
                descLabel.getStyleClass().add("subject-description"); // Reuse existing style

                subjectBox.getChildren().addAll(spacerLabel, codeLabel, descLabel, scheduleLabel);
                enrolledSubjectsDisplayContainer.getChildren().add(subjectBox);
            }
        } else {
            enrolledSubjectsDisplayContainer.getChildren().add(new Label("No subjects currently enrolled for this term."));
        }

        if (subjectLists.availableSubjects != null && !subjectLists.availableSubjects.isEmpty()) {
            for (SubjectData subject : subjectLists.availableSubjects()) {
                HBox subjectBox = new HBox(10); // spacing from FXML
                subjectBox.setAlignment(Pos.CENTER_LEFT);
                subjectBox.getStyleClass().add("subject-row"); // Style class from FXML example HBox
                // Padding is likely handled by the 'subject-row' style class or can be set if needed:
                // subjectBox.setPadding(new Insets(5.0, 0, 5.0, 0)); // Matches FXML example HBox padding

                CheckBox checkBox = new CheckBox();
                checkBox.getStyleClass().add("custom-checkbox"); // Style class from FXML
                HBox.setMargin(checkBox, new Insets(0, 0, 0, 5.0)); // Margin from FXML

                Label codeLabel = new Label(subject.subjectCode());
                codeLabel.setPrefWidth(200.0); // prefWidth from FXML
                codeLabel.getStyleClass().add("subject-code"); // Style class from FXML

                // Include units in the description label as it's not a separate column in FXML header
                Label descriptionLabel = new Label(String.format("%s (%d units)", subject.description(), subject.units()));
                descriptionLabel.setPrefWidth(300.0); // prefWidth from FXML
                descriptionLabel.setWrapText(true); // From FXML
                descriptionLabel.getStyleClass().add("subject-description"); // Style class from FXML

                ComboBox<String> scheduleComboBox = new ComboBox<>();
                scheduleComboBox.setPrefWidth(180.0); // prefWidth from FXML
                scheduleComboBox.setPrefHeight(30.0); // prefHeight from FXML
                scheduleComboBox.getStyleClass().add("modern-combo"); // Style class from FXML
                scheduleComboBox.setPromptText("Select Schedule"); // From FXML

                List<String> schedules = subject.availableSchedules();
                boolean hasActualSelectableSchedules = false;

                if (schedules != null && !schedules.isEmpty()) {
                    hasActualSelectableSchedules = schedules.stream().anyMatch(s ->
                        !s.equalsIgnoreCase("No schedules available") &&
                        !s.equalsIgnoreCase("No schedules available for this offering") &&
                        !s.equalsIgnoreCase("No specific schedules listed") &&
                        !s.equalsIgnoreCase("Schedule TBD")
                    );
                }

                if (hasActualSelectableSchedules) {
                    // Filter out placeholders if they might coexist, though current backend logic suggests they won't.
                    List<String> actualScheduleEntries = schedules.stream().filter(s ->
                        !s.equalsIgnoreCase("No schedules available") &&
                        !s.equalsIgnoreCase("No schedules available for this offering") &&
                        !s.equalsIgnoreCase("No specific schedules listed") &&
                        !s.equalsIgnoreCase("Schedule TBD")
                    ).collect(Collectors.toList());

                    if (!actualScheduleEntries.isEmpty()) {
                        scheduleComboBox.getItems().addAll(actualScheduleEntries);
                        scheduleComboBox.setValue(actualScheduleEntries.get(0));
                    } else {
                        // This case implies only placeholders were present, despite hasActualSelectableSchedules being true due to a logic flaw or unexpected data.
                        // Fallback to no schedules available.
                        scheduleComboBox.getItems().add("No schedules available");
                        scheduleComboBox.setValue("No schedules available");
                        hasActualSelectableSchedules = false; // Correct the flag
                    }
                } 
                // This 'else' covers cases where schedules list was null, empty, or only contained placeholders from the start.
                if (!hasActualSelectableSchedules) {
                    scheduleComboBox.getItems().clear(); // Ensure it's clean
                    scheduleComboBox.getItems().add("No schedules available");
                    scheduleComboBox.setValue("No schedules available");
                    scheduleComboBox.setDisable(true);
                    checkBox.setSelected(false); // Unselect if it was somehow selected
                    checkBox.setDisable(true);   // Disable the checkbox
                } else {
                    scheduleComboBox.setDisable(false);
                    checkBox.setDisable(false); // Ensure checkbox is enabled if schedules are present
                }

                subjectCheckboxes.add(checkBox);
                checkboxSubjectMap.put(checkBox, subject);
                subjectScheduleMap.put(checkBox, scheduleComboBox);

                checkBox.setOnAction(event -> {
                    updateEnrollButtonState();
                    updateSelectAllButtonState();
                });

                subjectBox.getChildren().addAll(checkBox, codeLabel, descriptionLabel, scheduleComboBox);
                subjectListContainer.getChildren().add(subjectBox);
            }
        } else {
            subjectListContainer.getChildren().add(new Label("No subjects available for enrollment for your program/semester, or you are already enrolled in all offered subjects."));
        }
        updateEnrollButtonState(); 
        updateSelectAllButtonState();
        if (selectAllButton != null) {
            // updateSelectAllButtonState already handles disabling if no enabled checkboxes exist.
            // So, this specific line might be redundant or can be simplified.
            // For now, let updateSelectAllButtonState handle the logic.
            // selectAllButton.setDisable(this.availableSubjects.isEmpty()); // Original line
            updateSelectAllButtonState(); // Ensure button text and state are correct after population
        }
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
    }

    private record EnrollmentData(String subjectId, String schedule) {}

    private record EnrollmentSubjectLists(List<SubjectData> enrolledSubjects, List<SubjectData> availableSubjects) {}

    private record SubjectData(String subjectCode, String description, int units, String subjectId, List<String> availableSchedules, String offeringId, String enrolledSchedule) {
        // Constructor for available subjects
        public SubjectData(String subjectCode, String description, int units, String subjectId, List<String> availableSchedules, String offeringId) {
            this(subjectCode, description, units, subjectId, availableSchedules, offeringId, null);
        }
        // Constructor for enrolled subjects
        public SubjectData(String subjectCode, String description, int units, String subjectId, String offeringId, String enrolledSchedule) {
            this(subjectCode, description, units, subjectId, Collections.emptyList(), offeringId, enrolledSchedule);
        }
    }

    private record StudentEnrollmentContext(int yearLevel, String semesterString, int studentId, String studentYearSection, int sectionId, int semesterId, int academicYearId) {}

    private void refreshEnrollmentView() {
        logger.debug("refreshEnrollmentView: Starting data refresh.");
        if (loadingIndicator != null) loadingIndicator.setVisible(true);
        if (enrollButton != null) enrollButton.setDisable(true);
        if (selectAllButton != null) selectAllButton.setDisable(true);
        // Clear previous data immediately for better UX
        if (subjectListContainer != null) subjectListContainer.getChildren().clear();
        if (enrolledSubjectsDisplayContainer != null) enrolledSubjectsDisplayContainer.getChildren().clear();
        if (currentYearLevelDisplayLabel != null) currentYearLevelDisplayLabel.setText("Loading...");
        if (currentSemesterDisplayLabel != null) currentSemesterDisplayLabel.setText("Loading...");

        Task<EnrollmentPageData> loadDataTask = new Task<>() {
            @Override
            protected EnrollmentPageData call() throws Exception {
                StudentEnrollmentContext context = fetchStudentEnrollmentContext();
                if (context == null) {
                    throw new IllegalStateException("Failed to fetch student enrollment context.");
                }
                EnrollmentSubjectLists subjectLists = fetchEnrollmentSubjectLists(context);
                return new EnrollmentPageData(context, subjectLists);
            }
        };

        loadDataTask.setOnSucceeded(event -> {
            EnrollmentPageData pageData = loadDataTask.getValue();
            if (pageData != null && pageData.context() != null) {
                logger.info("refreshEnrollmentView: Student context and subjects loaded: {}", pageData.context());
                if (currentYearLevelDisplayLabel != null) currentYearLevelDisplayLabel.setText(convertNumericYearToString(pageData.context().yearLevel()));
                if (currentSemesterDisplayLabel != null) currentSemesterDisplayLabel.setText(pageData.context().semesterString());
                populateSubjectListUI(pageData.subjectLists(), pageData.context());
            } else {
                // This case should ideally be handled by onFailed if an exception was thrown
                logger.error("refreshEnrollmentView: Task succeeded but returned null data or context.");
                handleLoadErrorUI("Task completed but data is missing.");
            }
            // loadingIndicator visibility and button states are handled by populateSubjectListUI or handleLoadErrorUI
        });

        loadDataTask.setOnFailed(event -> {
            Throwable ex = loadDataTask.getException();
            logger.error("refreshEnrollmentView: Failed to load enrollment data.", ex);
            handleLoadErrorUI(ex != null ? ex.getMessage() : "Unknown error during data loading.");
        });

        new Thread(loadDataTask).start();
    }

    private void handleLoadErrorUI(String errorMessage) {
        if (currentYearLevelDisplayLabel != null) currentYearLevelDisplayLabel.setText("N/A");
        if (currentSemesterDisplayLabel != null) currentSemesterDisplayLabel.setText("N/A");
        if (subjectListContainer != null) {
            subjectListContainer.getChildren().clear();
            subjectListContainer.getChildren().add(new Label("Error: " + errorMessage));
        }
        if (enrolledSubjectsDisplayContainer != null) enrolledSubjectsDisplayContainer.getChildren().clear();
        if (enrollButton != null) enrollButton.setDisable(true);
        if (selectAllButton != null) selectAllButton.setDisable(true);
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
        showAlert("Error Loading Data", "Could not load enrollment information: " + errorMessage, Alert.AlertType.ERROR);
    }

    private EnrollmentSubjectLists fetchEnrollmentSubjectLists(StudentEnrollmentContext context) throws SQLException {
        List<SubjectData> enrolledSubjects = new ArrayList<>();
        List<SubjectData> availableSubjectsOutput = new ArrayList<>(); // Final list for output

        // Phase 1: Query for ENROLLED subjects
        String enrolledQuery = "SELECT s.subject_code, s.description, s.units, s.subject_id, fl.load_id AS faculty_load_id, " +
                             "sch.days, sch.start_time, sch.end_time, r.room_name " +
                             "FROM subjects s " +
                             "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                             "JOIN student_load sl ON fl.load_id = sl.faculty_load " +
                             "LEFT JOIN schedule sch ON fl.load_id = sch.faculty_load_id " +
                             "LEFT JOIN room r ON sch.room_id = r.room_id " +
                             "WHERE sl.student_pk_id = ? AND fl.section_id = ? AND fl.semester_id = ? AND sl.academic_year_id = ?";

        logger.debug("fetchEnrollmentSubjectLists (Phase 1 - Enrolled): Query: {}, Params: [studentId={}, sectionId={}, semesterId={}, academicYearId={}]",
            enrolledQuery, context.studentId(), context.sectionId(), context.semesterId(), context.academicYearId());

        try (Connection connEnrolled = DBConnection.getConnection();
             PreparedStatement pstmtEnrolled = connEnrolled.prepareStatement(enrolledQuery)) {
            pstmtEnrolled.setInt(1, context.studentId());
            pstmtEnrolled.setInt(2, context.sectionId());
            pstmtEnrolled.setInt(3, context.semesterId());
            pstmtEnrolled.setInt(4, context.academicYearId());

            try (ResultSet rs = pstmtEnrolled.executeQuery()) {
                while (rs.next()) {
                    String scheduleStr = "Not Set";
                    if (rs.getString("days") != null && rs.getTime("start_time") != null && rs.getTime("end_time") != null) {
                        scheduleStr = String.format("%s %s-%s",
                                              rs.getString("days"),
                                              rs.getTime("start_time").toLocalTime().format(DateTimeFormatter.ofPattern("hh:mma")),
                                              rs.getTime("end_time").toLocalTime().format(DateTimeFormatter.ofPattern("hh:mma")));
                        if (rs.getString("room_name") != null) {
                            scheduleStr += " - " + rs.getString("room_name");
                        }
                    }
                    enrolledSubjects.add(new SubjectData(
                            rs.getString("subject_code"),
                            rs.getString("description"),
                            rs.getInt("units"),
                            String.valueOf(rs.getInt("subject_id")),
                            null, // For enrolled subjects, specific schedule is primary, not list
                            String.valueOf(rs.getInt("faculty_load_id")),
                            scheduleStr
                    ));
                }
            }
        }
        logger.debug("fetchEnrollmentSubjectLists (Phase 1 - Enrolled): Found {} enrolled subjects.", enrolledSubjects.size());

        // Helper record to store intermediate data for available subjects
        record AvailableSubjectInfo(String subjectCode, String description, int units, String subjectId, String facultyLoadId) {}
        List<AvailableSubjectInfo> tempAvailableSubjectInfos = new ArrayList<>();

        // Phase 2: Query for AVAILABLE subjects (Get subject details and faculty_load_id)
        String availableQuery = "SELECT s.subject_code, s.description, s.units, s.subject_id, " +
                                "fl.load_id AS faculty_load_id, " +
                                "(SELECT COUNT(*) FROM student_load sl_count WHERE sl_count.faculty_load = fl.load_id AND sl_count.academic_year_id = fl.academic_year_id AND sl_count.semester_id = fl.semester_id) AS current_enrollees " +
                                "FROM subjects s " +
                                "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                                "WHERE fl.section_id = ? AND fl.semester_id = ? " +
                                "AND fl.load_id NOT IN (" +
                                "  SELECT sl_inner.faculty_load FROM student_load sl_inner " +
                                "  WHERE sl_inner.student_pk_id = ? AND sl_inner.semester_id = ?" +
                                ") AND s.year_level = ? AND s.semester_id = ?"; // Corrected: s.year_level instead of s.year_level_id

        logger.debug("fetchEnrollmentSubjectLists (Phase 2 - Available Details): Query: {}, Params: [sectionId={}, semesterId={}, studentId={}, semesterIdSubQuery={}, yearLevel={}, semesterIdSubject={}]",
            availableQuery, context.sectionId(), context.semesterId(),
            context.studentId(), context.semesterId(), context.yearLevel(), context.semesterId());

        try (Connection connAvailableDetails = DBConnection.getConnection(); // Separate connection for available subject details
             PreparedStatement pstmtAvailable = connAvailableDetails.prepareStatement(availableQuery)) {
            
            pstmtAvailable.setInt(1, context.sectionId());
            pstmtAvailable.setInt(2, context.semesterId());
            pstmtAvailable.setInt(3, context.studentId());
            pstmtAvailable.setInt(4, context.semesterId());
            pstmtAvailable.setInt(5, context.yearLevel());
            pstmtAvailable.setInt(6, context.semesterId());

            try (ResultSet rs = pstmtAvailable.executeQuery()) {
                while (rs.next()) {
                    tempAvailableSubjectInfos.add(new AvailableSubjectInfo(
                            rs.getString("subject_code"),
                            rs.getString("description"),
                            rs.getInt("units"),
                            String.valueOf(rs.getInt("subject_id")),
                            String.valueOf(rs.getInt("faculty_load_id"))
                    ));
                }
            }
        } // connAvailableDetails and pstmtAvailable are closed here

        logger.debug("fetchEnrollmentSubjectLists (Phase 2 - Available Details): Found {} potential available subjects.", tempAvailableSubjectInfos.size());

        // Phase 3: For each available subject, get its schedules using another new connection
        if (!tempAvailableSubjectInfos.isEmpty()) {
            String scheduleQuery = "SELECT sch.days, sch.start_time, sch.end_time, r.room_name " +
                                   "FROM schedule sch " +
                                   "LEFT JOIN room r ON sch.room_id = r.room_id " +
                                   "WHERE sch.faculty_load_id = ?";
            logger.debug("fetchEnrollmentSubjectLists (Phase 3 - Schedules): Schedule Query: {}", scheduleQuery);

            try (Connection connSchedule = DBConnection.getConnection(); // New connection specifically for schedules
                 PreparedStatement pstmtSchedule = connSchedule.prepareStatement(scheduleQuery)) {
                
                for (AvailableSubjectInfo info : tempAvailableSubjectInfos) {
                    List<String> schedulesForSubject = new ArrayList<>();
                    pstmtSchedule.setInt(1, Integer.parseInt(info.facultyLoadId()));
                    
                    try (ResultSet rsSchedule = pstmtSchedule.executeQuery()) {
                        while (rsSchedule.next()) {
                            String scheduleStr = "Schedule TBD"; // Default if parts are null
                            Time startTime = rsSchedule.getTime("start_time");
                            Time endTime = rsSchedule.getTime("end_time");
                            String days = rsSchedule.getString("days");
                            String roomName = rsSchedule.getString("room_name");

                            if (days != null && startTime != null && endTime != null) {
                                scheduleStr = String.format("%s %s-%s",
                                        days,
                                        startTime.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mma")),
                                        endTime.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mma")));
                                if (roomName != null && !roomName.trim().isEmpty()) {
                                    scheduleStr += " - " + roomName;
                                }
                            } else if (days == null && startTime == null && endTime == null && (roomName == null || roomName.trim().isEmpty())) {
                                // If all schedule components are null or empty, it might mean no specific schedule is listed yet.
                                scheduleStr = "No specific schedules listed";
                            }
                            schedulesForSubject.add(scheduleStr);
                        }
                    }

                    if (schedulesForSubject.isEmpty()) {
                        // If no rows in schedule table for this faculty_load_id, add a placeholder
                        schedulesForSubject.add("No schedules available for this offering");
                    }

                    availableSubjectsOutput.add(new SubjectData(
                            info.subjectCode(),
                            info.description(),
                            info.units(),
                            info.subjectId(),
                            schedulesForSubject,
                            info.facultyLoadId()
                    ));
                }
            } // connSchedule and pstmtSchedule are closed here
        }

        logger.debug("fetchEnrollmentSubjectLists (Phase 3 - Schedules): Processed schedules for {} available subjects.", availableSubjectsOutput.size());

        List<SubjectData> allSubjects = new ArrayList<>(enrolledSubjects);
        allSubjects.addAll(availableSubjectsOutput);
        return new EnrollmentSubjectLists(enrolledSubjects, availableSubjectsOutput);
    }

    private record EnrollmentPageData(StudentEnrollmentContext context, EnrollmentSubjectLists subjectLists) {}

    private void showAlert(String title, String message, String type) {
        if ("error".equals(type)){
            showAlert(title, message, Alert.AlertType.ERROR);
        } else if ("info".equals(type)) {
            showAlert(title, message, Alert.AlertType.INFORMATION);
        } else {
            showAlert(title, message, Alert.AlertType.WARNING);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(alertType);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }

    private StudentEnrollmentContext fetchStudentEnrollmentContext() throws SQLException {
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();
        logger.debug("fetchStudentEnrollmentContext: currentUserIdentifier from RememberMeHandler: {}", currentUserIdentifier);

        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            logger.warn("fetchStudentEnrollmentContext: Current user identifier is null or empty.");
            throw new SQLException("Current user identifier not found.");
        }

        String sql = "SELECT " +
                "    sec.year_level, " +  
                "    sec.section_name AS student_year_section, " +
                "    sem.semester_name AS section_semester, " +
                "    s.student_id, " +
                "    sec.section_id, " +      
                "    sec.semester_id, " +     
                "    sec.academic_year_id " + 
                "FROM " +
                "    public.students s " +
                "LEFT JOIN " +
                "    public.section sec ON s.current_year_section_id = sec.section_id " + 
                "LEFT JOIN " +
                "    public.semesters sem ON sec.semester_id = sem.semester_id " +        
                "LEFT JOIN " +
                "    public.academic_years ay ON sec.academic_year_id = ay.academic_year_id " + 
                "WHERE " +
                "    s.student_number = ?";

        logger.debug("fetchStudentEnrollmentContext: SQL query: {}\n", sql.replace("\n", " ").replaceAll("\s+", " "));
        logger.debug("fetchStudentEnrollmentContext: Parameter: {}", currentUserIdentifier);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUserIdentifier);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int yearLevelInt = rs.getInt("year_level"); // Get integer year_level
                    String studentYearSectionStr = rs.getString("student_year_section");
                    String semester = rs.getString("section_semester");
                    int studentId = rs.getInt("student_id");
                    int sectionId = rs.getInt("section_id");
                    int semesterId = rs.getInt("semester_id");
                    int academicYearId = rs.getInt("academic_year_id");
                    StudentEnrollmentContext context = new StudentEnrollmentContext(yearLevelInt, semester, studentId, studentYearSectionStr, sectionId, semesterId, academicYearId);
                    logger.debug("fetchStudentEnrollmentContext: Fetched context: {}", context);
                    return context;
                } else {
                    logger.warn("fetchStudentEnrollmentContext: No enrollment context found for student identifier: {}", currentUserIdentifier);
                    return null; // Or throw specific exception
                }
            }
        } catch (SQLException e) {
            logger.error("SQL Error fetching student enrollment context: ", e);
            throw e;
        }
    }

    private String convertNumericYearToString(int yearLevel) {
        return switch (yearLevel) {
            case 1 -> "1st Year";
            case 2 -> "2nd Year";
            case 3 -> "3rd Year";
            case 4 -> "4th Year";
            case 5 -> "5th Year"; // If applicable
            default -> null;
        };
    }

    private String standardizeSemesterFormat(String semester) {
        if (semester == null) return null;
        String lowerSemester = semester.toLowerCase().trim();
        if (lowerSemester.contains("first") || lowerSemester.contains("1st")) return "1st Semester";
        if (lowerSemester.contains("second") || lowerSemester.contains("2nd")) return "2nd Semester";
        if (lowerSemester.contains("summer")) return "Summer";
        logger.error("Could not standardize semester: {}", semester);
        return null;
    }

    private String getCurrentAcademicYear() {
        // This should ideally come from a config or a dedicated table/logic
        return "2024-2025"; // Placeholder
    }

    private void updateDynamicUnitCountAndEnrollButtonState() {
        currentSelectedUnits = 0;
        boolean anySelected = false;
        for (CheckBox cb : subjectCheckboxes) {
            if (cb.isSelected()) {
                anySelected = true;
                SubjectData sd = checkboxSubjectMap.get(cb);
                if (sd != null && sd.units() != 0) {
                    currentSelectedUnits += sd.units();
                }
            }
        }

        if (unitCounterLabel != null) {
            unitCounterLabel.setText("Selected Units: " + currentSelectedUnits + "/" + MAX_UNITS);
            if (currentSelectedUnits > MAX_UNITS) {
                unitCounterLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); // Highlight if over limit
            } else {
                unitCounterLabel.setStyle("-fx-text-fill: -fx-text-base-color; -fx-font-weight: normal;"); // Default style
            }
        }

        if (enrollButton != null) {
            enrollButton.setDisable(!anySelected || currentSelectedUnits == 0 || currentSelectedUnits > MAX_UNITS);
        }

        // Advanced UX: Disable checkboxes that would exceed MAX_UNITS if selected
        for (CheckBox cb : subjectCheckboxes) {
            if (!cb.isSelected()) {
                SubjectData sd = checkboxSubjectMap.get(cb);
                if (sd != null && sd.units() != 0) {
                    if (currentSelectedUnits + sd.units() > MAX_UNITS) {
                        cb.setDisable(true);
                    } else {
                        cb.setDisable(false);
                    }
                }
            } else {
                 cb.setDisable(false); // Ensure selected checkboxes are always enabled (so they can be deselected)
            }
        }
        if (selectAllButton != null) {
            boolean allDisabled = subjectCheckboxes.stream().allMatch(Node::isDisabled);
            boolean allSelected = !subjectCheckboxes.isEmpty() && subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
            selectAllButton.setDisable(subjectCheckboxes.isEmpty() || allDisabled || allSelected);
        }
    }
}
