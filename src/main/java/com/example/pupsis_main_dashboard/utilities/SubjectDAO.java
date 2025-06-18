package com.example.pupsis_main_dashboard.utilities;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {


    /**
     * Retrieves all subject codes ordered alphabetically.
     *
     * @return List of subject codes as Strings.
     */
    public List<String> getAllSubjectCodes() {
        List<String> subjectCodes = new ArrayList<>();
        String sql = "SELECT subject_code FROM subjects ORDER BY subject_code ASC";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                subjectCodes.add(rs.getString("subject_code"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving subject codes:");
            e.printStackTrace();
        }
        return subjectCodes;
    }

    /**
     * Gets subject_id for a given subject code.
     *
     * @param subjectCode the subject code to search
     * @return the subject_id as int
     * @throws SQLException if subject code is not found or DB error occurs
     */
    public int getSubjectIdByCode(String subjectCode) throws SQLException {
        String sql = "SELECT subject_id FROM subjects WHERE subject_code = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, subjectCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("subject_id");
                } else {
                    throw new SQLException("Subject code not found: " + subjectCode);
                }
            }
        }
    }
}
