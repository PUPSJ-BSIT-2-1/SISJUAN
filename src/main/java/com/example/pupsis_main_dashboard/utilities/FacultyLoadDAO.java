package com.example.pupsis_main_dashboard.utilities;

import com.example.pupsis_main_dashboard.models.FacultyAssignment;
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
     * @param facultyId      the faculty identifier (int)
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

    /**
     * Retrieves all subject assignments for a given faculty (for the "Assigned Subjects" dialog)
     * Make sure your joined tables and columns match your schema.
     */
    public List<FacultyAssignment> getAssignmentsForFaculty(int facultyId) {
        List<FacultyAssignment> assignments = new ArrayList<>();
        String sql = """
        SELECT s.subject_code,
               s.description,
               sec.section_name,
               sem.semester_name,
               s.year_level
        FROM faculty_load fl
        JOIN subjects s ON fl.subject_id = s.subject_id
        JOIN section sec ON fl.section_id = sec.section_id
        JOIN semesters sem ON fl.semester_id = sem.semester_id
        WHERE fl.faculty_id = ?
    """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, facultyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FacultyAssignment assignment = new FacultyAssignment(
                        rs.getString("subject_code"),
                        rs.getString("description"),
                        rs.getString("section_name"),
                        rs.getString("semester_name"),
                        rs.getString("year_level")
                );
                assignments.add(assignment);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (getAssignmentsForFaculty): " + e.getMessage());
        }
        return assignments;
    }


    // Simple record to represent a faculty load entry.
    public record FacultyLoad(int facultyId, int subjectId, int sectionId, int semesterId, int academicYearId) {}

}
