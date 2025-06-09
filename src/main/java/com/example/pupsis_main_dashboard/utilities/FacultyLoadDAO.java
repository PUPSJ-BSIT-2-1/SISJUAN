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
     * @param facultyId      the faculty identifier (String)
     * @param subjectId      the subject identifier (int)
     * @param sectionId      the section identifier (int) from the 'section' table
     * @param semesterId     the semester identifier (int)
     * @param academicYearId the academic year identifier (int)
     * @return true if insert is successful, false otherwise
     */
    public boolean addFacultyLoad(String facultyId, int subjectId, int sectionId, int semesterId, int academicYearId) {
        String sql = "INSERT INTO faculty_load (faculty_id, subject_id, section_id, semester_id, academic_year_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, facultyId);
            stmt.setInt(2, subjectId);
            stmt.setInt(3, sectionId);
            stmt.setInt(4, semesterId);
            stmt.setInt(5, academicYearId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding faculty load: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
