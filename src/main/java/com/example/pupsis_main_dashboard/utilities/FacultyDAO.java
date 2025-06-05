package com.example.pupsis_main_dashboard.utilities;

import com.example.pupsis_main_dashboard.models.Faculty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {

    private final Connection connection;

    public FacultyDAO() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    // Add Faculty
    public boolean addFaculty(Faculty faculty) {
        // faculty_id is SERIAL, so it's auto-generated.
        String sql = "INSERT INTO faculty (faculty_number, firstname, middlename, lastname, department_id, email, contactnumber, birthdate, faculty_status_id, date_joined) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, faculty.getFacultyId()); // This is faculty_number
            stmt.setString(2, faculty.getFirstName());
            stmt.setString(3, faculty.getMiddleName());
            stmt.setString(4, faculty.getLastName());
            
            if (faculty.getDepartmentId() != null) {
                stmt.setInt(5, faculty.getDepartmentId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            stmt.setString(6, faculty.getEmail());
            stmt.setString(7, faculty.getContactNumber());
            stmt.setDate(8, faculty.getBirthdate() != null ? Date.valueOf(faculty.getBirthdate()) : null);
            
            if (faculty.getFacultyStatusId() != null) {
                stmt.setInt(9, faculty.getFacultyStatusId());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }
            stmt.setDate(10, faculty.getDateJoined() != null ? Date.valueOf(faculty.getDateJoined()) : null);

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (addFaculty): " + e.getMessage());
            e.printStackTrace(); // For more detailed error logging
            return false;
        }
    }

    // Retrieve all Faculty
    public List<Faculty> getAllFaculty() {
        List<Faculty> facultyList = new ArrayList<>();
        String sql = "SELECT f.faculty_id, f.faculty_number, f.firstname, f.middlename, f.lastname, " +
                     "f.email, f.contactnumber, f.birthdate, f.date_joined, " +
                     "f.department_id, d.department_name, " +
                     "f.faculty_status_id, fs.status_name AS faculty_status_name " +
                     "FROM faculty f " +
                     "LEFT JOIN departments d ON f.department_id = d.department_id " +
                     "LEFT JOIN faculty_statuses fs ON f.faculty_status_id = fs.faculty_status_id " +
                     "ORDER BY f.lastname ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Faculty faculty = new Faculty(
                        rs.getString("faculty_number"),      // facultyId (textual)
                        rs.getInt("faculty_id"),            // actualFacultyId (integer PK)
                        rs.getString("firstname"),
                        rs.getString("middlename"),
                        rs.getString("lastname"),
                        rs.getObject("department_id") != null ? rs.getInt("department_id") : null, // departmentId
                        rs.getString("department_name"),    // departmentName
                        rs.getString("email"),
                        rs.getString("contactnumber"),
                        rs.getDate("birthdate") != null ? rs.getDate("birthdate").toLocalDate() : null,
                        rs.getObject("faculty_status_id") != null ? rs.getInt("faculty_status_id") : null, // facultyStatusId
                        rs.getString("faculty_status_name"),// facultyStatusName
                        rs.getDate("date_joined") != null ? rs.getDate("date_joined").toLocalDate() : null
                );
                facultyList.add(faculty);
            }

        } catch (SQLException e) {
            System.err.println("SQL Error (getAllFaculty): " + e.getMessage());
            e.printStackTrace(); // For more detailed error logging
        }

        return facultyList;
    }

    // Update Faculty
    public boolean updateFaculty(Faculty faculty) {
        String sql = "UPDATE faculty SET faculty_number = ?, firstname = ?, middlename = ?, lastname = ?, " +
                     "department_id = ?, email = ?, contactnumber = ?, birthdate = ?, " +
                     "faculty_status_id = ?, date_joined = ? " +
                     "WHERE faculty_id = ?"; // Use actual integer PK for WHERE clause

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, faculty.getFacultyId()); // faculty_number
            stmt.setString(2, faculty.getFirstName());
            stmt.setString(3, faculty.getMiddleName());
            stmt.setString(4, faculty.getLastName());

            if (faculty.getDepartmentId() != null) {
                stmt.setInt(5, faculty.getDepartmentId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.setString(6, faculty.getEmail());
            stmt.setString(7, faculty.getContactNumber());
            stmt.setDate(8, faculty.getBirthdate() != null ? Date.valueOf(faculty.getBirthdate()) : null);

            if (faculty.getFacultyStatusId() != null) {
                stmt.setInt(9, faculty.getFacultyStatusId());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }
            stmt.setDate(10, faculty.getDateJoined() != null ? Date.valueOf(faculty.getDateJoined()) : null);
            
            // WHERE clause parameter
            if (faculty.getActualFacultyId() != null) { // actualFacultyId is the integer PK
                 stmt.setInt(11, faculty.getActualFacultyId());
            } else {
                // This case should ideally not happen for an update if actualFacultyId is the PK
                System.err.println("Error: actualFacultyId is null for update.");
                return false;
            }

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (updateFaculty): " + e.getMessage());
            e.printStackTrace(); // For more detailed error logging
            return false;
        }
    }

    // Delete Faculty
    public boolean deleteFaculty(int actualFacultyId) { // Changed parameter to int
        String sql = "DELETE FROM faculty WHERE faculty_id = ?"; // Use actual integer PK

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, actualFacultyId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (deleteFaculty): " + e.getMessage());
            e.printStackTrace(); // For more detailed error logging
            return false;
        }
    }
}
