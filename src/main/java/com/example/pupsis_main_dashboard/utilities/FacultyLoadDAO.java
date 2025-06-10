package com.example.pupsis_main_dashboard.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FacultyLoadDAO {

    private final Connection connection;

    public FacultyLoadDAO() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Adds a faculty load entry (assignment of a subject to faculty).
     *
     * @param facultyId      the faculty identifier (String)
     * @param subjectId      the subject identifier (int)
     * @param sectionId      the section identifier (int) from the 'section' table
     * @param semesterId     the semester identifier (int)
     * @param academicYearId the academic year identifier (int)
     * @return true if insert is successful, false otherwise
     */
    public boolean addFacultyLoad(int facultyId, int subjectId, int sectionId, int semesterId, int academicYearId) {
        String sql = "INSERT INTO faculty_load (faculty_id, subject_id, section_id, semester_id, academic_year_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, facultyId);
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

    /**
     * Retrieves all faculty load entries from the database.
     *
     * @return a list of all faculty load entries
     * @throws SQLException if a database access error occurs
     */
    public List<FacultyLoad> getAllFacultyLoad() throws SQLException {
        List<FacultyLoad> facultyLoads = new ArrayList<>();
        String sql = "SELECT faculty_id, subject_id, section_id, semester_id, academic_year_id FROM faculty_load";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                FacultyLoad load = new FacultyLoad(
                        rs.getInt("faculty_id"),
                        rs.getInt("subject_id"),
                        rs.getInt("section_id"),
                        rs.getInt("semester_id"),
                        rs.getInt("academic_year_id")
                );
                facultyLoads.add(load);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving faculty loads: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return facultyLoads;
    }

    public record FacultyLoad(int facultyId, int subjectId, int sectionId, int semesterId, int academicYearId) {

    }
}
