package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent; 
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
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

    @FXML private VBox root;
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
    private List<SubjectData> enrolledSubjects = new ArrayList<>();
    private List<CheckBox> subjectCheckboxes = new ArrayList<>();
    private Map<CheckBox, SubjectData> checkboxSubjectMap = new HashMap<>();
    private Map<CheckBox, ComboBox<String>> subjectScheduleMap = new HashMap<>();
    private int currentSelectedUnits = 0;

    private StudentDashboardController studentDashboardController;

    public void setStudentDashboardController(StudentDashboardController controller) {
        this.studentDashboardController = controller;
    }


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
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
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

                    if (selectedScheduleDisplayString == null || selectedScheduleDisplayString.equals("No schedules available") || selectedScheduleDisplayString.equals("No schedules available for this offering") || selectedScheduleDisplayString.equals("No specific schedules listed") || selectedScheduleDisplayString.equals("Schedule TBD")) {
                        showAlert("Invalid Selection", "Please select a valid schedule for " + subjectData.subjectCode() + ".", Alert.AlertType.WARNING);
                        return null;
                    }
                    // The EnrollmentData's 'schedule' field is used as faculty_load_id in the INSERT query.
                    // For enrolled subjects, use enrolledSchedule, for available subjects use offeringId
                    String scheduleId = subjectData.enrolledSchedule() != null ? subjectData.enrolledSchedule() : subjectData.offeringId();
                    
                    if (scheduleId == null) {
                        // Check if this is an available subject with no faculty load
                        if (subjectData.offeringId() == null) {
                            logger.error("Subject {} is not available for enrollment: No faculty load assigned", subjectData.subjectCode());
                            showAlert("Invalid Selection", "Subject " + subjectData.subjectCode() + " is not available for enrollment. Please contact your academic advisor.", Alert.AlertType.WARNING);
                            return null;
                        }
                        logger.error("Invalid subject data: Both enrolledSchedule and offeringId are null for subject {}", subjectData.subjectCode());
                        throw new IllegalStateException("Invalid subject data: No schedule ID available for subject " + subjectData.subjectCode());
                    }
                    return new EnrollmentData(subjectData.subjectId(), scheduleId);
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

                    // Updated INSERT: Remove load_id as PK, use composite PK, and check for duplicates
                    String insertSql = "INSERT INTO student_load (student_pk_id, subject_id, semester_id, academic_year_id, faculty_load, section_id) VALUES (?, ?, ?, ?, ?, ?)";
                    String duplicateCheckSql = "SELECT 1 FROM student_load WHERE student_pk_id = ? AND subject_id = ? AND semester_id = ? AND academic_year_id = ?";

                    try (PreparedStatement stmt = connection.prepareStatement(insertSql);
                         PreparedStatement dupStmt = connection.prepareStatement(duplicateCheckSql)) {
                        for (EnrollmentData enrollment : selectedSubjects) {
                            try {
                                String subjectIdStr = enrollment.subjectId();
                                String scheduleStr = enrollment.schedule();

                                if (subjectIdStr == null || scheduleStr == null) {
                                    logger.error("Invalid enrollment data: subjectId='{}' or schedule='{}' is null", subjectIdStr, scheduleStr);
                                    throw new SQLException("Invalid enrollment data: Subject ID or Schedule ID cannot be null");
                                }

                                int subjectId = Integer.parseInt(subjectIdStr);
                                int facultyLoad = Integer.parseInt(scheduleStr);

                                // Set parameters for duplicate check (dupStmt)
                                dupStmt.setInt(1, studentDbId);
                                dupStmt.setInt(2, subjectId);
                                dupStmt.setInt(3, currentContext.semesterId());
                                dupStmt.setInt(4, currentContext.academicYearId());
                                try (ResultSet rs = dupStmt.executeQuery()) {
                                    if (rs.next()) {
                                        logger.warn("Duplicate enrollment detected for student_pk_id={}, subject_id={}, semester_id={}, academic_year_id={}",
                                                studentDbId, subjectId, currentContext.semesterId(), currentContext.academicYearId());
                                        continue; // Skip duplicate
                                    }
                                }

                                // Set parameters for insert (stmt)
                                stmt.setInt(1, studentDbId); // student_pk_id
                                stmt.setInt(2, subjectId); // subject_id
                                stmt.setInt(3, currentContext.semesterId()); // semester_id
                                stmt.setInt(4, currentContext.academicYearId()); // academic_year_id
                                stmt.setInt(5, facultyLoad); // faculty_load (offeringId)
                                stmt.setInt(6, currentContext.sectionId()); // section_id

                                logger.debug("Adding to batch: student_pk_id={}, subject_id={}, semester_id={}, academic_year_id={}, faculty_load={}, section_id={}",
                                        studentDbId, subjectId, currentContext.semesterId(), currentContext.academicYearId(), facultyLoad, currentContext.sectionId());
                                stmt.addBatch();
                            } catch (NumberFormatException e) {
                                logger.error("Invalid enrollment data: subjectId='{}' or schedule='{}' cannot be parsed as integers", enrollment.subjectId(), enrollment.schedule(), e);
                                throw new SQLException("Invalid enrollment data: Subject ID or Schedule ID is not a valid number", e);
                            }
                        }
                        stmt.executeBatch();
                    }
                    connection.commit();
                    SessionData.getInstance().setUnitsEnrolled(currentSelectedUnits);
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
                redirectToPaymentInfo();
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

    private void redirectToPaymentInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Redirect to Payment Information");
        alert.setHeaderText("Do you want to go to the Payment Information section?");
        alert.setContentText("Make sure you have enrolled all the subjects you want before proceeding.");
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // User confirmed: redirect and set controller
            try {
                ScrollPane contentPane = (ScrollPane) root.getScene().lookup("#contentPane");

                if (contentPane != null) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/example/pupsis_main_dashboard/fxml/StudentPaymentInfo.fxml")
                    );

                    Parent newContent = loader.load();
                    StudentPaymentInfoController controller = loader.getController();
                    controller.setEnrollmentController(this);

                    contentPane.setContent(newContent);
                    studentDashboardController.handleQuickActionClicks("/com/example/pupsis_main_dashboard/fxml/StudentPaymentInfo.fxml");
                }
            } catch (IOException e) {
                logger.error("Error loading StudentPaymentInfo.fxml: {}", e.getMessage());
                StageAndSceneUtils.showAlert("Navigation Error",
                        "Unable to load payment information. Please try again.", Alert.AlertType.ERROR);
            }
        }
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
        // Filter checkboxes to only those corresponding to subjects with actual, selectable schedules.
        List<CheckBox> selectableCheckboxes = subjectCheckboxes.stream()
                .filter(cb -> {
                    SubjectData subject = checkboxSubjectMap.get(cb);
                    if (subject == null || subject.availableSchedules() == null || subject.availableSchedules().isEmpty()) {
                        return false;
                    }
                    // A schedule is considered actual if it's not a placeholder message.
                    return subject.availableSchedules().stream().anyMatch(s ->
                        !s.equalsIgnoreCase("No schedules available") &&
                        !s.equalsIgnoreCase("No schedules available for this offering") &&
                        !s.equalsIgnoreCase("No specific schedules listed") &&
                        !s.equalsIgnoreCase("Schedule TBD")
                    );
                })
                .collect(Collectors.toList());

        if (selectableCheckboxes.isEmpty()) {
            return; // No subjects with schedules to select.
        }

        // Determine if all selectable subjects are already selected to toggle between select/deselect all.
        boolean allSelected = selectableCheckboxes.stream().allMatch(CheckBox::isSelected);
        boolean targetState = !allSelected;

        // Apply the target state to all selectable checkboxes.
        for (CheckBox cb : selectableCheckboxes) {
            cb.setSelected(targetState);
        }

        // Refresh UI states.
        updateSelectAllButtonState();
        updateDynamicUnitCountAndEnrollButtonState();
    }

    private void populateSubjectListUI(EnrollmentSubjectLists subjectLists, StudentEnrollmentContext context) {
        logger.debug("populateSubjectListUI: Populating UI with {} enrolled and {} available subjects.", 
            subjectLists.enrolledSubjects().size(), subjectLists.availableSubjects().size());
        subjectCheckboxes.clear();
        checkboxSubjectMap.clear();
        subjectScheduleMap.clear();

        this.availableSubjects = subjectLists.availableSubjects;
        this.enrolledSubjects = subjectLists.enrolledSubjects;

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
                    codeLabel.setStyle("-fx-text-fill: gray;");
                    descriptionLabel.setStyle("-fx-text-fill: gray;");
                    checkBox.setDisable(true);
                } else {
                    scheduleComboBox.setDisable(false);
                    checkBox.setDisable(false); // Ensure checkbox is enabled if schedules are present
                    codeLabel.setDisable(false);
                    descriptionLabel.setDisable(false);
                }

                subjectCheckboxes.add(checkBox);
                checkboxSubjectMap.put(checkBox, subject);
                subjectScheduleMap.put(checkBox, scheduleComboBox);

                checkBox.setOnAction(event -> {
                    updateDynamicUnitCountAndEnrollButtonState();
                    updateSelectAllButtonState();
                });

                subjectBox.getChildren().addAll(checkBox, codeLabel, descriptionLabel, scheduleComboBox);
                subjectListContainer.getChildren().add(subjectBox);
            }
        } else {
            subjectListContainer.getChildren().add(new Label("No subjects available for enrollment for your program/semester, or you are already enrolled in all offered subjects."));
        }
        updateDynamicUnitCountAndEnrollButtonState(); 
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
        String enrolledQuery = "SELECT s.subject_code, s.description, CASE WHEN s.units ~ '^[0-9]+$' THEN CAST(s.units AS INTEGER) ELSE 0 END AS units, s.subject_id, fl.load_id AS faculty_load_id, " +
                             "sch.days, sch.start_time, sch.end_time, r.room_name " +
                             "FROM subjects s " +
                             "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                             "JOIN student_load sl ON fl.load_id = sl.faculty_load " +
                             "LEFT JOIN schedule sch ON fl.load_id = sch.faculty_load_id " +
                             "LEFT JOIN room r ON sch.room_id = r.room_id " +
                             "WHERE sl.student_pk_id = ? AND fl.semester_id = ? AND sl.academic_year_id = ?";

        logger.debug("fetchEnrollmentSubjectLists (Phase 1 - Enrolled): Query: {}, Params: [studentId={}, semesterId={}, academicYearId={}]",
            enrolledQuery, context.studentId(), context.semesterId(), context.academicYearId());

        try (Connection connEnrolled = DBConnection.getConnection();
             PreparedStatement pstmtEnrolled = connEnrolled.prepareStatement(enrolledQuery)) {
            pstmtEnrolled.setInt(1, context.studentId());
            pstmtEnrolled.setInt(2, context.semesterId());
            pstmtEnrolled.setInt(3, context.academicYearId());

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

        // Phase 2: Query for AVAILABLE subjects (Get subject details)
        String availableQuery = "SELECT DISTINCT s.subject_id, s.subject_code, s.description, CASE WHEN s.units ~ '^[0-9]+$' THEN CAST(s.units AS INTEGER) ELSE 0 END AS units, fl.load_id AS faculty_load_id " +
                "FROM subjects s " +
                "JOIN faculty_load fl ON s.subject_id = fl.subject_id " +
                "WHERE fl.semester_id = ? " +
                "AND s.year_level = ? " +
                "AND s.semester_id = ? " +
                "AND fl.section_id = ? " +
                "AND s.subject_id NOT IN (" +
                "  SELECT subject_id FROM student_load sl WHERE sl.student_pk_id = ? AND sl.semester_id = ?" +
                ")";

        logger.info("[DEBUG] Available Subjects Query Params: semesterId={}, yearLevel={}, semesterIdSubject={}, sectionId={}, studentId={}, semesterIdSubQuery={}",
            context.semesterId(), context.yearLevel(), context.semesterId(), context.sectionId(), context.studentId(), context.semesterId());

        logger.debug("fetchEnrollmentSubjectLists (Phase 2 - Available Details): Query: {}, Params: [semesterId={}, yearLevel={}, semesterIdSubject={}, sectionId={}, studentId={}, semesterIdSubQuery={}]",
            availableQuery, context.semesterId(), context.yearLevel(), context.semesterId(), context.sectionId(), context.studentId(), context.semesterId());

        try (Connection connAvailableDetails = DBConnection.getConnection(); // Separate connection for available subject details
             PreparedStatement pstmtAvailable = connAvailableDetails.prepareStatement(availableQuery)) {
            
            pstmtAvailable.setInt(1, context.semesterId());
            pstmtAvailable.setInt(2, context.yearLevel());
            pstmtAvailable.setInt(3, context.semesterId());
            pstmtAvailable.setInt(4, context.sectionId());
            pstmtAvailable.setInt(5, context.studentId());
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
                            info.facultyLoadId(),
                            null
                    ));
                }
            } // connSchedule and pstmtSchedule are closed here
        }

        logger.debug("fetchEnrollmentSubjectLists (Phase 3 - Schedules): Processed schedules for {} available subjects.", availableSubjectsOutput.size());

        // Deduplicate available subjects by subjectId, merge schedules for each subject
        Map<String, SubjectData> uniqueAvailableSubjects = new LinkedHashMap<>();
        for (SubjectData subject : availableSubjectsOutput) {
            if (uniqueAvailableSubjects.containsKey(subject.subjectId())) {
                // Merge schedules if duplicate
                List<String> existingSchedules = uniqueAvailableSubjects.get(subject.subjectId()).availableSchedules();
                Set<String> mergedSchedules = new LinkedHashSet<>(existingSchedules);
                mergedSchedules.addAll(subject.availableSchedules());
                uniqueAvailableSubjects.get(subject.subjectId()).availableSchedules().clear();
                uniqueAvailableSubjects.get(subject.subjectId()).availableSchedules().addAll(mergedSchedules);
            } else {
                uniqueAvailableSubjects.put(subject.subjectId(), subject);
            }
        }
        List<SubjectData> dedupedAvailableSubjects = new ArrayList<>(uniqueAvailableSubjects.values());
        logger.debug("fetchEnrollmentSubjectLists: Deduped available subjects count: {}", dedupedAvailableSubjects.size());
        return new EnrollmentSubjectLists(enrolledSubjects, dedupedAvailableSubjects);
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
                "    sec.section_id, " + // <--- add this line
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
                    int sectionId = rs.getInt("section_id"); // <--- get correct section id
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
        if (unitCounterLabel == null) return;

        int selectedUnits = subjectCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .mapToInt(cb -> checkboxSubjectMap.get(cb).units())
                .sum();

        int enrolledUnits = 0;
        if (this.enrolledSubjects != null) {
            enrolledUnits = this.enrolledSubjects.stream()
                    .mapToInt(SubjectData::units)
                    .sum();
        }

        currentSelectedUnits = selectedUnits + enrolledUnits;

        unitCounterLabel.setText(String.format("Selected Units: %d/%d", currentSelectedUnits, MAX_UNITS));

        if (currentSelectedUnits > MAX_UNITS) {
            unitCounterLabel.setStyle("-fx-text-fill: red;");
            enrollButton.setDisable(true);
        } else {
            unitCounterLabel.setStyle(""); // Revert to default style
            // Enable enroll button only if at least one subject is selected and units are within limit.
            enrollButton.setDisable(selectedUnits == 0);
        }
    }
}
