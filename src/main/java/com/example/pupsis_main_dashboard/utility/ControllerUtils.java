package com.example.pupsis_main_dashboard.utility;

import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import javafx.animation.TranslateTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ControllerUtils {
    public static void animateVBox(VBox vbox, double translationX) {
        TranslateTransition animation = new TranslateTransition(Duration.millis(300), vbox);
        animation.setToX(translationX);
        animation.play();
    }

    public static void animateBlur(Pane pane, boolean enableBlur) {
        if (enableBlur) {
            GaussianBlur blur = new GaussianBlur(10);
            pane.setEffect(blur);
        } else {
            pane.setEffect(null);
        }
    }

    public static String getStudentFullName(String identifier, boolean isEmail) {
        if (identifier == null || identifier.isEmpty()) return "";
        
        String query = isEmail 
            ? "SELECT firstname, middlename, lastname FROM students WHERE email = ?"
            : "SELECT firstname, middlename, lastname FROM students WHERE student_id = ?";
            
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, identifier);
            ResultSet result = statement.executeQuery();
            
            if (result.next()) {
                String firstName = result.getString("firstname");
                String middleName = result.getString("middlename");
                String lastName = result.getString("lastname");
                // Convert middle name to initial if not empty
                String middleInitial = middleName != null && !middleName.isEmpty() 
                    ? middleName.charAt(0) + "."
                    : "";
                return String.format("%s %s %s", firstName, middleInitial, lastName).trim();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
}
