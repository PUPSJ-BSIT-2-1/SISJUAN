package com.sisjuan.utilities;

import com.sisjuan.models.Department;
import com.sisjuan.models.FacultyStatus;
import com.sisjuan.models.Faculty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {

    // (Removed: private final Connection connection;)

    public FacultyDAO() {
        // No need to initialize a connection field.
    }

    // Helper: Check if a faculty_number already exists
    public boolean facultyNumberExists(String facultyNumber) {
        String sql = "SELECT COUNT(*) FROM faculty WHERE faculty_number = ?";
        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, facultyNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error (facultyNumberExists): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Add Faculty with duplicate check: returns 1 = added, 0 = SQL error, -1 = duplicate
    public int addFaculty(Faculty faculty) {
        String sql = "INSERT INTO faculty (faculty_number, firstname, middlename, lastname, department_id, email, contactnumber, birthdate, faculty_status_id, date_joined) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, faculty.getFacultyId());
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

            return stmt.executeUpdate(); // 1 for success
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                // SQLState 23505 = unique_violation (PostgreSQL)
                System.err.println("Duplicate Faculty Number: " + e.getMessage());
                return -1;
            }
            System.err.println("SQL Error (addFaculty): " + e.getMessage());
            e.printStackTrace();
            return 0;
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
        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
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
            e.printStackTrace();
        }
        return facultyList;
    }

    // Update Faculty
    public boolean updateFaculty(Faculty faculty) {
        String sql = "UPDATE faculty SET faculty_number = ?, firstname = ?, middlename = ?, lastname = ?, " +
                "department_id = ?, email = ?, contactnumber = ?, birthdate = ?, " +
                "faculty_status_id = ?, date_joined = ? " +
                "WHERE faculty_id = ?"; // Use actual integer PK for WHERE clause

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
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

            if (faculty.getActualFacultyId() != null) {
                stmt.setInt(11, faculty.getActualFacultyId());
            } else {
                System.err.println("Error: actualFacultyId is null for update.");
                return false;
            }

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("SQL Error (updateFaculty): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Delete Faculty
    public int deleteFaculty(int actualFacultyId) {
        String sql = "DELETE FROM faculty WHERE faculty_id = ?";
        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, actualFacultyId);
            int affected = stmt.executeUpdate();
            return affected; // 1 if deleted, 0 if not found
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                return -1; // Foreign key violation (still referenced)
            }
            System.err.println("SQL Error (deleteFaculty): " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    // Department List
    public static List<Department> getAllDepartments() throws SQLException {
        List<Department> departments = new ArrayList<>();
        String sql = "SELECT department_id, department_name FROM departments ORDER BY department_name ASC";
        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                departments.add(new Department(
                        rs.getInt("department_id"),
                        rs.getString("department_name")
                ));
            }
        }
        return departments;
    }

    // FacultyStatus List
    public static List<FacultyStatus> getAllFacultyStatuses() throws SQLException {
        List<FacultyStatus> statuses = new ArrayList<>();
        String sql = "SELECT faculty_status_id, status_name FROM faculty_statuses ORDER BY status_name ASC";
        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                statuses.add(new FacultyStatus(
                        rs.getInt("faculty_status_id"),
                        rs.getString("status_name")
                ));
            }
        }
        return statuses;
    }
}
