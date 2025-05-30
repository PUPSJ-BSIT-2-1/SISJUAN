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
        String sql = "INSERT INTO faculty (faculty_id, firstname, middlename, lastname, department, email, contactnumber, birthdate, status, date_joined) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(faculty.getFacultyId()));
            stmt.setString(2, faculty.getFirstName());
            stmt.setString(3, faculty.getMiddleName());
            stmt.setString(4, faculty.getLastName());
            stmt.setString(5, faculty.getDepartment());
            stmt.setString(6, faculty.getEmail());
            stmt.setString(7, faculty.getContactNumber());
            stmt.setDate(8, Date.valueOf(faculty.getBirthdate()));
            stmt.setString(9, faculty.getStatus());
            stmt.setDate(10, faculty.getDateJoined() != null ? Date.valueOf(faculty.getDateJoined()) : null);

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (addFaculty): " + e.getMessage());
            return false;
        }
    }

    // Retrieve all Faculty
    public List<Faculty> getAllFaculty() {
        List<Faculty> facultyList = new ArrayList<>();
        String sql = "SELECT faculty_id, firstname, middlename, lastname, department, email, contactnumber, birthdate, status, date_joined FROM faculty ORDER BY lastname ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Faculty faculty = new Faculty(
                        rs.getString("faculty_id"),
                        rs.getString("firstname"),
                        rs.getString("middlename"),
                        rs.getString("lastname"),
                        rs.getString("department"),
                        rs.getString("email"),
                        rs.getString("contactnumber"),
                        rs.getDate("birthdate").toLocalDate(),
                        rs.getString("status"),
                        rs.getDate("date_joined") != null ? rs.getDate("date_joined").toLocalDate() : null
                );
                facultyList.add(faculty);
            }

        } catch (SQLException e) {
            System.err.println("SQL Error (getAllFaculty): " + e.getMessage());
        }

        return facultyList;
    }

    // Update Faculty
    public boolean updateFaculty(Faculty faculty) {
        String sql = "UPDATE faculty SET firstname = ?, middlename = ?, lastname = ?, department = ?, email = ?, contactnumber = ?, birthdate = ?, status = ?, date_joined = ? " +
                "WHERE faculty_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, faculty.getFirstName());
            stmt.setString(2, faculty.getMiddleName());
            stmt.setString(3, faculty.getLastName());
            stmt.setString(4, faculty.getDepartment());
            stmt.setString(5, faculty.getEmail());
            stmt.setString(6, faculty.getContactNumber());
            stmt.setDate(7, Date.valueOf(faculty.getBirthdate()));
            stmt.setString(8, faculty.getStatus());
            stmt.setDate(9, faculty.getDateJoined() != null ? Date.valueOf(faculty.getDateJoined()) : null);
            stmt.setInt(10, Integer.parseInt(faculty.getFacultyId()));

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (updateFaculty): " + e.getMessage());
            return false;
        }
    }

    // Delete Faculty
    public boolean deleteFaculty(String facultyId) {
        String sql = "DELETE FROM faculty WHERE faculty_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(facultyId));
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (deleteFaculty): " + e.getMessage());
            return false;
        }
    }
}
