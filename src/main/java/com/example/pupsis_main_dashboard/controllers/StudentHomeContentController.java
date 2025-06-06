package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import com.example.pupsis_main_dashboard.utilities.RememberMeHandler;
import com.example.pupsis_main_dashboard.utilities.SessionData;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;
import java.util.List;
import java.util.ArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentHomeContentController {

    @FXML
    private Label studentNameLabel;
    @FXML
    private Button viewGradesButton;
    @FXML
    private Button viewScheduleButton;
    @FXML
    private Button viewPaymentButton; //TODO: Add payment button
    @FXML
    private Button seeEnrollmentButton;
    @FXML
    private Label yearLevel;
    @FXML
    private Label semester;
    @FXML
    private Label status;
    @FXML
    private Label semGPA;
    @FXML
    private Label totalSubjects;
    @FXML
    private Label numAnnouncements;
    @FXML
    private ListView<String> listAnnouncements;

    private static final String GRADES_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentGrades.fxml";
    private static final String SCHEDULE_FXML = "/com/example/pupsis_main_dashboard/fxml/RoomAssignment.fxml";
    private static final String ENROLLMENT_FXML = "/com/example/pupsis_main_dashboard/fxml/StudentEnrollmentContent.fxml";

    private static final Logger logger = LoggerFactory.getLogger(StudentHomeContentController.class);

    private StudentDashboardController studentDashboardController;

    // Method to set the reference to StudentDashboardController
    public void setStudentDashboardController(StudentDashboardController controller) {
        this.studentDashboardController = controller;

        // Set up the viewGradesButton click event
        if (viewGradesButton != null) {
            viewGradesButton.setOnAction(this::viewGradesButtonClick);
        }

        // Set up the viewScheduleButton click event
        if (viewScheduleButton != null) {
            viewScheduleButton.setOnAction(this::viewScheduleButtonClick);
        }

        if (seeEnrollmentButton != null) {
            seeEnrollmentButton.setOnAction(this::seeEnrollmentButtonClick);
        }
    }

    // Initializes the home content by loading the student's credentials,
    // extracting their full name, and displaying their first name on the label.
    // If an error occurs, it sets the label to a default value ("Student").
    @FXML
    public void initialize() {
        // Set initial placeholder text for all labels that will be loaded
        studentNameLabel.setText("Loading...");
        yearLevel.setText("Loading...");
        semester.setText("Loading...");
        status.setText("Loading...");
        semGPA.setText("Loading...");
        totalSubjects.setText("Loading...");
        numAnnouncements.setText("Loading...");
        listAnnouncements.getItems().clear(); // Clear previous items
        // Consider adding a ProgressIndicator as well

        Task<HomeContentData> loadHomeDataTask = new Task<>() {
            @Override
            protected HomeContentData call() throws Exception {
                String identifier = RememberMeHandler.getCurrentUserStudentNumber();
                if (identifier == null || identifier.isEmpty()) {
                    throw new IllegalStateException("No user logged in.");
                }

                // Fetch all data in the background
                String fullStudentName = StudentLoginController.getStudentFullName(identifier);
                String studentFirstName = getStudentFirstName(fullStudentName);

                String studentNumberForQueries = SessionData.getInstance().getStudentNumber();
                if (studentNumberForQueries == null || studentNumberForQueries.isEmpty()) {
                    studentNumberForQueries = fetchStudentNumber(identifier);
                    if (studentNumberForQueries != null) {
                        SessionData.getInstance().setStudentNumber(studentNumberForQueries);
                    } else {
                        throw new SQLException("Could not determine student number for queries.");
                    }
                }

                String yearLevelText = determineCurrentYearLevelLogic(studentNumberForQueries);
                SemesterInfo semesterInfo = determineCurrentSemesterInfoLogic(studentNumberForQueries);
                String statusText = determineCurrentStatusLogic(studentNumberForQueries);
                int currentAcademicYearId = com.example.pupsis_main_dashboard.utilities.SchoolYearAndSemester.getCurrentAcademicYearId();
                String totalSubjectsText = calculateTotalSubjectsLogic(studentNumberForQueries, currentAcademicYearId, semesterInfo.id());
                String gwaText = calculateCurrentGWALogic();
                List<String> announcements = populateAnnouncementsLogic();
                String numAnnouncementsText = String.valueOf(announcements.size());

                return new HomeContentData(studentFirstName, fullStudentName, yearLevelText, semesterInfo, statusText, gwaText, totalSubjectsText, announcements, numAnnouncementsText);
            }
        };

        loadHomeDataTask.setOnSucceeded(_ -> {
            HomeContentData data = loadHomeDataTask.getValue();

            studentNameLabel.setText(data.studentFirstName);
            yearLevel.setText(data.yearLevelText);
            semester.setText(data.semesterInfo.name()); // Use name from SemesterInfo
            status.setText(data.statusText);
            semGPA.setText(data.gwaText);
            totalSubjects.setText(data.totalSubjectsText);
            listAnnouncements.getItems().setAll(data.announcements);
            numAnnouncements.setText(data.numAnnouncementsText);

            logger.info("Home content loaded for student: {} (identifier: {})", 
                        data.fullStudentName, RememberMeHandler.getCurrentUserStudentNumber());
        });

        loadHomeDataTask.setOnFailed(_ -> {
            Throwable ex = loadHomeDataTask.getException();
            logger.error("Error initializing home content: {}", ex.getMessage(), ex);
            // Set labels to error or default state
            studentNameLabel.setText("Error");
            yearLevel.setText("N/A");
            semester.setText("N/A");
            status.setText("N/A");
            semGPA.setText("N/A");
            totalSubjects.setText("N/A");
            numAnnouncements.setText("N/A");
            listAnnouncements.getItems().setAll("Failed to load announcements.");
        });

        new Thread(loadHomeDataTask).start();
    }

    private String getStudentFirstName(String fullStudentName) {
        String studentFirstName = "Student"; // Default
        if (fullStudentName != null && fullStudentName.contains(",")) {
            String[] nameParts = fullStudentName.split(",");
            studentFirstName = nameParts.length > 1 ? nameParts[1].trim().split(" ")[0] : nameParts[0].trim();
            studentFirstName = studentFirstName.substring(0, 1).toUpperCase() + studentFirstName.substring(1).toLowerCase();
        } else if (fullStudentName != null) {
            studentFirstName = fullStudentName; // Use the full name if parsing fails
        }
        return studentFirstName;
    }

    // Helper method to fetch student number if not in session (implement if needed)
    private String fetchStudentNumber(String identifier) throws SQLException {
        String query = "SELECT student_number FROM students WHERE student_number = ?";
        
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, identifier);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("student_number");
            }
        }
        return null;
    }

    // Handler for viewGradesButton click
    private void viewGradesButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("View Grades button clicked, loading grades content");
            studentDashboardController.loadContent(GRADES_FXML);
            studentDashboardController.handleQuickActionClicks(GRADES_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load grades content");
        }
    }

    // Handler for viewScheduleButton click
    private void viewScheduleButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("View Schedule button clicked, loading schedule content");
            studentDashboardController.loadContent(SCHEDULE_FXML);
            studentDashboardController.handleQuickActionClicks(SCHEDULE_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load schedule content");
        }
    }

    // Handler for seeEnrollmentButton click
    private void seeEnrollmentButtonClick(ActionEvent event) {
        if (studentDashboardController != null) {
            logger.info("See Enrollment button clicked, loading enrollment content");
            studentDashboardController.loadContent(ENROLLMENT_FXML);
            studentDashboardController.handleQuickActionClicks(ENROLLMENT_FXML);
        } else {
            logger.error("StudentDashboardController reference is null, cannot load enrollment content");
        }
    }

    // Method to determine current year level
    private String determineCurrentYearLevelLogic(String studentNumber) {
        String query = """
            SELECT ys.year_section
            FROM public.students s
            JOIN public.year_section ys ON s.current_year_section_id = ys.section_id
            WHERE s.student_number = ?;
            """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String yearSectionText = rs.getString("year_section"); // e.g., "BSIT 1-1"
                if (yearSectionText != null && !yearSectionText.isEmpty()) {
                    // Basic parsing: Assumes a format like "COURSE X-Y"
                    // More robust parsing might be needed depending on actual year_section formats
                    String[] parts = yearSectionText.split(" ");
                    if (parts.length > 1) {
                        String yearPart = parts[1].split("-")[0]; // Takes the 'X' from "X-Y"
                        return switch (yearPart) {
                            case "1" -> "1st Year";
                            case "2" -> "2nd Year";
                            case "3" -> "3rd Year";
                            case "4" -> "4th Year";
                            // Add more cases if needed (e.g., 5th year for some courses)
                            default -> yearPart + "th Year"; // Fallback
                        };
                    }
                    return yearSectionText; // Fallback to a full text if parsing fails
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving year level for student {}: {}", studentNumber, e.getMessage());
        }
        return "N/A";
    }

    // Helper record for semester information
    private record SemesterInfo(String name, int id) {
        // Default values for safety, though actual logic should prevent their direct use if a DB call fails.
        public static final SemesterInfo DEFAULT = new SemesterInfo("N/A", 0);
    }

    // Placeholder method for determining current semester
    private SemesterInfo determineCurrentSemesterInfoLogic(String studentNumber) {
        String query = """
            SELECT sem.semester_name, sem.semester_id
            FROM public.students s
            JOIN public.year_section ys ON s.current_year_section_id = ys.section_id
            JOIN public.semesters sem ON ys.semester_id = sem.semester_id
            WHERE s.student_number = ?;
            """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("semester_name");
                int id = rs.getInt("semester_id");
                return new SemesterInfo(name, id);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving semester info for student {}: {}", studentNumber, e.getMessage());
        }
        return SemesterInfo.DEFAULT; // Return default N/A, 0 if error or not found
    }

    // Placeholder method for determining current status
    private String determineCurrentStatusLogic(String studentNumber) {
        // Using scholastic_status_id as requested
        String query = """
            SELECT ss.status_name
            FROM public.students s
            JOIN public.scholastic_statuses ss ON s.scholastic_status_id = ss.scholastic_status_id
            WHERE s.student_number = ?;
            """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("status_name");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving status for student {}: {}", studentNumber, e.getMessage());
        }
        return "N/A";
    }

    // Placeholder method for calculating total subjects
    private String calculateTotalSubjectsLogic(String studentNumber, int academicYearId, int semesterId) {
        // First, get student_id (PK) from student_number
        int studentPkId = -1;
        String studentIdQuery = "SELECT student_id FROM public.students WHERE student_number = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmtStudentId = conn.prepareStatement(studentIdQuery)) {
            pstmtStudentId.setString(1, studentNumber);
            try (ResultSet rsStudentId = pstmtStudentId.executeQuery()) {
                if (rsStudentId.next()) {
                    studentPkId = rsStudentId.getInt("student_id");
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching student_pk_id for {}: {}", studentNumber, e.getMessage());
            return "N/A";
        }

        if (studentPkId == -1) {
            logger.warn("Could not find student_pk_id for student_number: {}", studentNumber);
            return "N/A";
        }

        // academicYearId and semesterId are now passed as parameters
        if (academicYearId == 0 || semesterId == 0) { // Assuming 0 is an invalid/default ID
             logger.warn("Invalid academic year/semester ID (academicYearId: {}, semesterId: {}) for subject count.", academicYearId, semesterId);
             return "N/A";
        }

        String query = """
            SELECT COUNT(*) AS subject_count
            FROM public.student_load sl
            WHERE sl.student_pk_id = ?
              AND sl.academic_year_id = ?
              AND sl.semester_id = ?;
            """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, studentPkId);
            stmt.setInt(2, academicYearId);
            stmt.setInt(3, semesterId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getInt("subject_count"));
            }
        } catch (SQLException e) {
            logger.error("Error calculating total subjects for student {}: {}", studentNumber, e.getMessage());
        }
        return "N/A";
    }

    // Placeholder method for calculating current GWA
    private String calculateCurrentGWALogic() {
        double gwa = StudentGradesController.getGWA();
        return gwa == 0 ? "N/A" : String.format("%.2f", gwa);
    }

    // Method to populate announcements
    private List<String> populateAnnouncementsLogic() {
        List<String> announcements = new ArrayList<>();
        String query = "SELECT announcement FROM public.announcement ORDER BY date DESC LIMIT 5;"; // Corrected table name
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                announcements.add(rs.getString("announcement"));
            }
        } catch (SQLException e) {
            logger.error("Error populating announcements: {}", e.getMessage());
            announcements.add("Failed to load announcements.");
        }
        return announcements;
    }

    // Static inner class to hold data for the home content UI
    private static class HomeContentData {
        final String studentFirstName;
        final String fullStudentName;
        final String yearLevelText;
        final SemesterInfo semesterInfo; // Changed from String to SemesterInfo
        final String statusText;
        final String gwaText;
        final String totalSubjectsText;
        final List<String> announcements;
        final String numAnnouncementsText;

        public HomeContentData(String studentFirstName, String fullStudentName, String yearLevelText, 
                               SemesterInfo semesterInfo, String statusText, String gwaText, 
                               String totalSubjectsText, List<String> announcements, String numAnnouncementsText) {
            this.studentFirstName = studentFirstName;
            this.fullStudentName = fullStudentName;
            this.yearLevelText = yearLevelText;
            this.semesterInfo = semesterInfo;
            this.statusText = statusText;
            this.gwaText = gwaText;
            this.totalSubjectsText = totalSubjectsText;
            this.announcements = announcements;
            this.numAnnouncementsText = numAnnouncementsText;
        }
    }
}
