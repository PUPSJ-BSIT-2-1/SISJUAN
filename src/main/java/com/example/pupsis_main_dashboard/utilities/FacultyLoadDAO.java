package com.example.pupsis_main_dashboard.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FacultyLoadDAO {

    private final Connection connection;

    public FacultyLoadDAO() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Adds a faculty load entry (assignment of subject to faculty).
     *
     * @param facultyId    the faculty identifier (int)
     * @param subjectId    the subject identifier (int)
     * @param yearSection  the year_section string (e.g., "1-1")
     * @param semester     the semester (e.g., "1st Semester")
     * @param academicYear the academic year (e.g., "2023-2024")
     * @return true if insert is successful, false otherwise
     */
    public boolean addFacultyLoad(int facultyId, int subjectId, String yearSection, String semester, String academicYear) {
        String sql = "INSERT INTO faculty_load (faculty_id, subject_id, year_section, semester, academic_year) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, facultyId);
            stmt.setInt(2, subjectId);
            stmt.setString(3, yearSection);
            stmt.setString(4, semester);
            stmt.setString(5, academicYear);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding faculty load:");
            e.printStackTrace();
            return false;
        }
    }
}
