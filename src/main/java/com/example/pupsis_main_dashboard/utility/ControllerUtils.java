package com.example.pupsis_main_dashboard.utility;

import com.example.pupsis_main_dashboard.databaseOperations.DBConnection;
import javafx.animation.TranslateTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerUtils {
    private static final Logger logger = LoggerFactory.getLogger(ControllerUtils.class);

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
            ? "SELECT firstName, middleName, lastName FROM students WHERE email = ?"
            : "SELECT firstName, middleName, lastName FROM students WHERE student_id = ?";
            
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, identifier);
            ResultSet result = statement.executeQuery();
            
            if (result.next()) {
                String firstName = result.getString("firstName");
                String middleName = result.getString("middleName");
                String lastName = result.getString("lastName");
                String middleInitial = middleName != null && !middleName.isEmpty() 
                    ? middleName.charAt(0) + "."
                    : "";
                return String.format("%s, %s %s", lastName, firstName, middleInitial).trim();
            }
        } catch (SQLException e) {
            logger.error("Error getting student full name", e);
        }
        return "";
    }
}
