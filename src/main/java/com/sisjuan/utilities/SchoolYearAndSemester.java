package com.sisjuan.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SchoolYearAndSemester {

    public static final int DEFAULT_ID = -1;
    private static final String DEFAULT_NAME = "N/A - Configuration Error";

    /**
     * Fetches the currently active semester ID from the current_sem table.
     * This ID is assumed to be set by an administrator.
     * @return The current semester_id, or DEFAULT_ID if not found or error.
     */
    public static int getCurrentSemesterId() {
        String query = "SELECT active_semester_id FROM public.current_sem WHERE config_id = 1"; // Query updated for current_sem table
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("active_semester_id");
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error fetching current semester ID from current_sem table: " + e.getMessage());
            e.printStackTrace();
        }
        return DEFAULT_ID;
    }

    /**
     * Determines the name of the current semester (e.g., "1st Semester")
     * based on the current_semester_id from current_sem.
     * @return The name of the current semester, or DEFAULT_NAME if not found or error.
     */
    public static String determineCurrentSemester() {
        int semesterId = getCurrentSemesterId();
        if (semesterId == DEFAULT_ID) {
            return DEFAULT_NAME;
        }

        String query = "SELECT semester_name FROM public.semesters WHERE semester_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, semesterId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("semester_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching semester name for ID " + semesterId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return DEFAULT_NAME;
    }

    /**
     * Fetches the academic_year_id for the current semester.
     * It first gets the current_semester_id, then looks up its academic_year_id in the 'semesters' table.
     * @return The academic_year_id of the current semester, or DEFAULT_ID if error.
     */
    public static int getCurrentAcademicYearId() {
        int currentSemesterId = getCurrentSemesterId();
        if (currentSemesterId == DEFAULT_ID) {
            return DEFAULT_ID;
        }

        String query = "SELECT academic_year_id FROM public.semesters WHERE semester_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentSemesterId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("academic_year_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching academic_year_id for semester_id " + currentSemesterId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return DEFAULT_ID;
    }

    /**
     * Fetches the name of the current academic year (e.g., "2023-2024")
     * based on the current_academic_year_id.
     * @return The name of the current academic year, or DEFAULT_NAME if not found or error.
     */
    public static String getCurrentAcademicYear() {
        int academicYearId = getCurrentAcademicYearId();
        if (academicYearId == DEFAULT_ID) {
            return DEFAULT_NAME;
        }

        String query = "SELECT academic_year_name FROM public.academic_years WHERE academic_year_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, academicYearId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("academic_year_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching academic year name for ID " + academicYearId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return DEFAULT_NAME;
    }

    /**
     * Utility method to get the ID of a semester given its name.
     * (Kept from original, might still be useful elsewhere)
     * @param semesterName The name of the semester (e.g., "1st Semester").
     * @return The corresponding semester_id, or -1 if not found or input is null.
     */
    public static int getSemesterId(String semesterName) {
        if (semesterName == null) {
            return -1; // Or DEFAULT_ID, but -1 was original behavior for this specific method
        }
        // This switch assumes fixed IDs for names. 
        // For a more dynamic approach, this could also query the 'semesters' table.
        switch (semesterName) {
            case "1st Semester":
                return 1;
            case "Summer Semester": 
                return 2;
            case "2nd Semester":  
                return 3;
            default:
                // Fallback to query the database if name not in switch or if IDs are not fixed
                String query = "SELECT semester_id FROM public.semesters WHERE semester_name = ?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, semesterName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("semester_id");
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error fetching semester ID for name '" + semesterName + "': " + e.getMessage());
                    e.printStackTrace();
                }
                return -1; // Original default for not found
        }
    }
}
