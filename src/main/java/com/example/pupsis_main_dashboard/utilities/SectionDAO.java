package com.example.pupsis_main_dashboard.utilities;

import com.example.pupsis_main_dashboard.controllers.AdminAssignSubjectDialogController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SectionDAO {
    private final Connection connection;

    public SectionDAO() throws SQLException {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Fetches all sections from the 'section' table.
     * Assumes 'section' table has 'section_id' (INT) and 'section_name' (TEXT/VARCHAR).
     * Uses the AdminAssignSubjectDialogController.SectionItem record for the items.
     *
     * @return A list of SectionItem objects, or an empty list if an error occurs or no sections are found.
     */
    public List<AdminAssignSubjectDialogController.SectionItem> getAllSectionItems() {
        List<AdminAssignSubjectDialogController.SectionItem> sections = new ArrayList<>();
        // Assuming the section table has columns: section_id (INT), section_name (TEXT/VARCHAR)
        String sql = "SELECT section_id, section_name FROM section ORDER BY section_name ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int sectionId = rs.getInt("section_id");
                String sectionName = rs.getString("section_name");
                sections.add(new AdminAssignSubjectDialogController.SectionItem(sectionId, sectionName));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sections: " + e.getMessage());
            e.printStackTrace();
            // Return an empty list in case of error, or handle more gracefully
        }
        return sections;
    }

    // TODO: Add other necessary DAO methods for section management if needed
    // e.g., getSectionById, addSection, updateSection, deleteSection
}
