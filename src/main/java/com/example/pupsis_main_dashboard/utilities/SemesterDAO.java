package com.example.pupsis_main_dashboard.utilities;

import com.example.pupsis_main_dashboard.controllers.AdminAssignSubjectDialogController.SemesterItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SemesterDAO {

    private Connection connection;

    public SemesterDAO() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    public List<SemesterItem> getAllSemesters() throws SQLException {
        List<SemesterItem> semesters = new ArrayList<>();
        String sql = "SELECT semester_id, semester_name FROM semesters ORDER BY semester_id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                semesters.add(new SemesterItem(rs.getInt("semester_id"), rs.getString("semester_name")));
            }
        }
        return semesters;
    }
}
