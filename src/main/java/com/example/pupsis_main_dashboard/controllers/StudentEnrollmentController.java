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

    @FXML private VBox subjectListContainer;
    @FXML private VBox enrolledSubjectsDisplayContainer; 
    @FXML private Button selectAllButton;
    @FXML private Button enrollButton;
    @FXML private Label currentYearLevelDisplayLabel;
    @FXML private Label currentSemesterDisplayLabel;
    @FXML private ProgressIndicator loadingIndicator; 

    private StudentEnrollmentContext studentEnrollmentContext; 
    private List<SubjectData> availableSubjects;
    private List<CheckBox> subjectCheckboxes = new ArrayList<>();
    private Map<CheckBox, SubjectData> checkboxSubjectMap = new HashMap<>();
    private Map<CheckBox, ComboBox<String>> subjectScheduleMap = new HashMap<>();

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
        refreshEnrollmentView(); // Initial data load and view setup
    }

    @FXML
    private void handleEnrollment(ActionEvent event) { 
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

                    // student_load: student_id, subject_id, semester, load_id (PK), academic_year, year_section, faculty_load (FK to faculty_load.load_id)
                    String insertSql = "INSERT INTO student_load (student_id, subject_id, semester, load_id, academic_year, year_section, faculty_load) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                        for (EnrollmentData enrollment : selectedSubjects) {
                            stmt.setInt(1, studentDbId); // student_id from students table
                            stmt.setInt(2, Integer.parseInt(enrollment.subjectId())); // subject_id from subjects table
                            stmt.setString(3, currentSemesterForDB);
                            stmt.setInt(4, nextLoadId++); // PK for student_load
                            stmt.setString(5, academicYear);
                            stmt.setString(6, studentCurrentYearSection); // Student's current year_section
                            stmt.setInt(7, Integer.parseInt(enrollment.schedule())); // This 'schedule' from EnrollmentData is actually faculty_load_id
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
            boolean allSelected = !subjectCheckboxes.isEmpty() && subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
            selectAllButton.setText(allSelected ? "Deselect All" : "Select All");
            selectAllButton.setDisable(subjectCheckboxes.isEmpty()); // Disable if no subjects to select
        }
    }

    @FXML
    private void handleSelectAll(ActionEvent event) {
        boolean allCurrentlySelected = subjectCheckboxes.stream().allMatch(CheckBox::isSelected);
        boolean targetState = !allCurrentlySelected;

        for (CheckBox cb : subjectCheckboxes) {
            cb.setSelected(targetState);
        }
        updateSelectAllButtonState(); // Update button text
        updateEnrollButtonState(); // Update enroll button based on new selection
    }

    private void updateEnrollButtonState() {
        if (enrollButton != null) {
            boolean anySelected = subjectCheckboxes.stream().anyMatch(CheckBox::isSelected);
            enrollButton.setDisable(!anySelected);
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
                HBox subjectBox = new HBox(10);
                subjectBox.setPadding(new Insets(5));
                subjectBox.setAlignment(Pos.CENTER_LEFT);

                CheckBox checkBox = new CheckBox();
                Label subjectLabel = new Label(String.format("%s - %s (%d units)",
                        subject.subjectCode(), subject.description(), subject.units()));
                subjectLabel.setMinWidth(300); 
                subjectLabel.setWrapText(true);

                ComboBox<String> scheduleComboBox = new ComboBox<>();
                if (subject.availableSchedules() != null && !subject.availableSchedules().isEmpty()) {
                    scheduleComboBox.getItems().addAll(subject.availableSchedules());
                    scheduleComboBox.setValue(subject.availableSchedules().get(0)); 
                } else {
                    scheduleComboBox.getItems().add("No schedules available");
                    scheduleComboBox.setValue("No schedules available");
                    scheduleComboBox.setDisable(true);
                }
                scheduleComboBox.setMinWidth(150); 

                subjectCheckboxes.add(checkBox);
                checkboxSubjectMap.put(checkBox, subject);
                subjectScheduleMap.put(checkBox, scheduleComboBox);

                checkBox.setOnAction(event -> {
                    updateEnrollButtonState();
                    updateSelectAllButtonState();
                });

                subjectBox.getChildren().addAll(checkBox, subjectLabel, scheduleComboBox);
                subjectListContainer.getChildren().add(subjectBox);
            }
        } else {
            subjectListContainer.getChildren().add(new Label("No subjects available for enrollment for your program/semester, or you are already enrolled in all offered subjects."));
        }
        updateEnrollButtonState(); 
        updateSelectAllButtonState();
        if (selectAllButton != null) {
            selectAllButton.setDisable(this.availableSubjects.isEmpty()); // Corrected variable name
            updateSelectAllButtonState(); // Ensure button text is correct
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

    private record StudentEnrollmentContext(String yearLevelString, String semesterString, int studentId, String studentYearSection, int sectionId, int semesterId, int academicYearId) {}

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
                if (currentYearLevelDisplayLabel != null) currentYearLevelDisplayLabel.setText(pageData.context().yearLevelString());
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
        showAlert("Error Loading Data", "Could not load enrollment information: " + errorMessage,"error");
    }

    private EnrollmentSubjectLists fetchEnrollmentSubjectLists(StudentEnrollmentContext context) throws SQLException {
        List<SubjectData> enrolledSubjects = new ArrayList<>();
        List<SubjectData> availableSubjects = new ArrayList<>();

        // Query for ENROLLED subjects
        String enrolledQuery = "SELECT s.subject_code, s.description, s.units, s.subject_id, fl.load_id AS faculty_load_id, " +
                             "sch.days, sch.start_time, sch.end_time, r.room_name " +
                             "FROM subjects s " +
                             "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                             "JOIN student_load sl ON fl.load_id = sl.faculty_load " +
                             "LEFT JOIN schedule sch ON fl.load_id = sch.faculty_load_id " +
                             "LEFT JOIN room r ON sch.room_id = r.room_id " +
                             "WHERE sl.student_pk_id = ? AND fl.section_id = ? AND fl.semester_id = ? AND sl.academic_year_id = ?";

        logger.debug("fetchEnrollmentSubjectLists: Enrolled Query: {}, Params: [studentId={}, sectionId={}, semesterId={}, academicYearId={}]", 
            enrolledQuery, context.studentId(), context.sectionId(), context.semesterId(), context.academicYearId());

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtEnrolled = conn.prepareStatement(enrolledQuery)) {
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
        logger.debug("fetchEnrollmentSubjectLists: Found {} enrolled subjects.", enrolledSubjects.size());

        // Query for AVAILABLE subjects
        // Fetches subjects from faculty_load for the student's section and semester,
        // excluding those the student is already enrolled in for that semester (regardless of academic year of enrollment).
        // Offerings from any academic year matching section and semester will be considered.
        String availableQuery = "SELECT s.subject_code, s.description, s.units, s.subject_id, " +
                                "fl.load_id AS faculty_load_id, " +
                                "(SELECT COUNT(*) FROM student_load sl_count WHERE sl_count.faculty_load = fl.load_id AND sl_count.academic_year_id = fl.academic_year_id AND sl_count.semester_id = fl.semester_id) AS current_enrollees " +
                                "FROM subjects s " +
                                "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                                "WHERE fl.section_id = ? AND fl.semester_id = ? " +
                                "AND fl.load_id NOT IN (" +
                                "  SELECT sl_inner.faculty_load FROM student_load sl_inner " +
                                "  WHERE sl_inner.student_pk_id = ? AND sl_inner.semester_id = ?" +
                                ")";

        logger.debug("fetchEnrollmentSubjectLists: Available Query: {}, Params: [sectionId={}, semesterId={}, studentId={}, semesterIdSubQuery={}]", 
            availableQuery, context.sectionId(), context.semesterId(), 
            context.studentId(), context.semesterId());

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtAvailable = conn.prepareStatement(availableQuery)) {
            
            pstmtAvailable.setInt(1, context.sectionId());       // for fl.section_id
            pstmtAvailable.setInt(2, context.semesterId());      // for fl.semester_id
            // Parameter for fl.academic_year_id removed
            pstmtAvailable.setInt(3, context.studentId());       // for sl_inner.student_pk_id in subquery (was 4)
            pstmtAvailable.setInt(4, context.semesterId());      // for sl_inner.semester_id in subquery (was 5)
            // Parameter for sl_inner.academic_year_id removed (was 6)

            try (ResultSet rs = pstmtAvailable.executeQuery()) {
                while (rs.next()) {
                    // For available subjects, current_enrollees is fetched by the query.
                    // MAX_ENROLLEES is a class constant.
                    // These are not part of the SubjectData record itself, so they are not passed to its constructor.
                    // They can be used when building the UI row for this subject if needed.
                    // int currentEnrollees = rs.getInt("current_enrollees"); // Value is available here

                    availableSubjects.add(new SubjectData(
                            rs.getString("subject_code"),
                            rs.getString("description"),
                            rs.getInt("units"),
                            String.valueOf(rs.getInt("subject_id")),
                            java.util.Collections.emptyList(), // availableSchedules
                            String.valueOf(rs.getInt("faculty_load_id")) // offeringId
                    ));
                }
            }
        }
        logger.debug("fetchEnrollmentSubjectLists: Found {} available subjects.", availableSubjects.size());

        List<SubjectData> allSubjects = new ArrayList<>(enrolledSubjects);
        allSubjects.addAll(availableSubjects);
        return new EnrollmentSubjectLists(enrolledSubjects, availableSubjects);
    }

    private record EnrollmentPageData(StudentEnrollmentContext context, EnrollmentSubjectLists subjectLists) {}

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

    private void showAlert(String title, String message,String type) {
        if (type == "error"){
            showAlert(title, message, Alert.AlertType.ERROR);
        } else if (type == "info") {
            showAlert(title, message, Alert.AlertType.INFORMATION);
        } else {
            showAlert(title, message, Alert.AlertType.WARNING);
        }
    }

    private StudentEnrollmentContext fetchStudentEnrollmentContext() throws SQLException {
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();
        logger.debug("fetchStudentEnrollmentContext: currentUserIdentifier from RememberMeHandler: {}", currentUserIdentifier);

        if (currentUserIdentifier == null || currentUserIdentifier.isEmpty()) {
            logger.warn("fetchStudentEnrollmentContext: Current user identifier is null or empty.");
            throw new SQLException("Current user identifier not found.");
        }

        String sql = """
            SELECT
                ys.year_section AS student_year_section,
                sem.semester_name AS section_semester,
                s.student_id,
                ys.section_id,
                sem.semester_id,
                ay.academic_year_id
            FROM
                public.students s
            LEFT JOIN
                public.year_section ys ON s.current_year_section_id = ys.section_id
            LEFT JOIN
                public.semesters sem ON ys.semester_id = sem.semester_id
            LEFT JOIN
                public.academic_years ay ON ys.academic_year_id = ay.academic_year_id
            WHERE
                s.student_number = ?
            """;

        logger.debug("fetchStudentEnrollmentContext: SQL query: {}", sql);
        logger.debug("fetchStudentEnrollmentContext: Parameter: {}", currentUserIdentifier);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUserIdentifier);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String yearSection = rs.getString("student_year_section");
                    String semester = rs.getString("section_semester");
                    int studentId = rs.getInt("student_id");
                    int sectionId = rs.getInt("section_id");
                    int semesterId = rs.getInt("semester_id");
                    int academicYearId = rs.getInt("academic_year_id");
                    StudentEnrollmentContext context = new StudentEnrollmentContext(yearSection, semester, studentId, yearSection, sectionId, semesterId, academicYearId);
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

    private String convertYearSectionToYearLevel(String yearSection) {
        if (yearSection == null || yearSection.isEmpty()) return null;
        String digits = yearSection.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            char yearChar = digits.charAt(0);
            return convertNumericYearToString(Character.getNumericValue(yearChar));
        }
        if (yearSection.toLowerCase().contains("year")) return yearSection;
        logger.error("Could not parse year level from year_section: {}", yearSection);
        return null;
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
}
