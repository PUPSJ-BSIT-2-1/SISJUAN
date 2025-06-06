package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminHomeContentController {

    @FXML
    private VBox rootVBox;

    @FXML
    private Label facultyNameLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label totalStudentsLabel;

    @FXML
    private Label totalFacultyLabel;

    @FXML
    private Label totalCoursesLabel;

    @FXML
    private Label enrollmentCountLabel;

    @FXML
    private Label pendingActionsLabel;

    @FXML
    private Label academicCalendarLabel;

    // TODO: Add ListView for recent activity, documents, and events if needed
    // @FXML
    // private ListView<?> recentActivityListView;

    // @FXML
    // private ListView<?> documentsListView;

    // @FXML
    // private ListView<?> upcomingEventsListView;

    public void initialize() {
        // Set the current date
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        // Populate data from the database or services
        loadDashboardData();

        // TODO: Populate these labels with actual data from the database or services
        facultyNameLabel.setText("Admin User"); // Placeholder
        // totalStudentsLabel.setText("0"); // Will be set by loadDashboardData()
        // totalFacultyLabel.setText("0"); // Will be set by loadDashboardData()
        // totalCoursesLabel.setText("0"); // Will be set by loadDashboardData()
        enrollmentCountLabel.setText("0%");
        pendingActionsLabel.setText("0");
        academicCalendarLabel.setText("N/A");
    }

    private void loadDashboardData() {

        // Load total students
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            String studentSql = "SELECT COUNT(*) AS student_count FROM public.students";
            ResultSet studentRs = stmt.executeQuery(studentSql);

            if (studentRs.next()) {
                int studentCount = studentRs.getInt("student_count");
                totalStudentsLabel.setText(String.valueOf(studentCount));
            } else {
                totalStudentsLabel.setText("N/A");
            }
            studentRs.close(); // Close ResultSet

            // Load total faculty
            String facultySql = "SELECT COUNT(*) AS faculty_count FROM public.faculty";
            ResultSet facultyRs = stmt.executeQuery(facultySql);

            if (facultyRs.next()) {
                int facultyCount = facultyRs.getInt("faculty_count");
                totalFacultyLabel.setText(String.valueOf(facultyCount));
            } else {
                totalFacultyLabel.setText("N/A");
            }
            facultyRs.close(); // Close ResultSet

            // Load total courses
            String coursesSql = "SELECT COUNT(*) AS course_count FROM public.subjects";
            ResultSet coursesRs = stmt.executeQuery(coursesSql);

            if (coursesRs.next()) {
                int courseCount = coursesRs.getInt("course_count");
                totalCoursesLabel.setText(String.valueOf(courseCount));
            } else {
                totalCoursesLabel.setText("N/A");
            }
            coursesRs.close(); // Close ResultSet

        } catch (SQLException e) {
            e.printStackTrace(); // Consider more sophisticated error handling
            totalStudentsLabel.setText("Error");
            totalFacultyLabel.setText("Error");
            totalCoursesLabel.setText("Error");
        }

        // TODO: Load other data (enrollment count, etc.) here
    }

    // TODO: Add methods to handle actions for quick action buttons if they are interactive

}
