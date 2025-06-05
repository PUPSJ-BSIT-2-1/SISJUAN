package com.example.pupsis_main_dashboard.utilities;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.mail.MessagingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class NotificationManager {
    private static NotificationManager instance;
    private final List<Notification> notifications = new ArrayList<>();
    private final Preferences prefs;
    private final EmailService emailService;
    private final ExecutorService emailExecutor;
    
    private NotificationManager() {
        prefs = Preferences.userNodeForPackage(NotificationManager.class);
        emailService = new EmailService();
        emailExecutor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
    
    public void addNotification(String title, String message, NotificationType type) {
        Notification notification = new Notification(title, message, type);
        notifications.add(notification);
        
        // Check if we should send email notification based on user preferences
        boolean shouldSendEmail = false;
        
        switch (type) {
            case EMAIL:
                shouldSendEmail = prefs.getBoolean("emailNotifications", true);
                break;
            case GRADE:
                shouldSendEmail = prefs.getBoolean("gradeNotifications", true);
                break;
            case ANNOUNCEMENT:
                shouldSendEmail = prefs.getBoolean("announcementNotifications", false);
                break;
            case SYSTEM:
                // System notifications don't send emails by default
                break;
        }
        
        // Send email if it's enabled and not a system notification
        if (type != NotificationType.SYSTEM && shouldSendEmail && prefs.getBoolean("emailNotifications", true)) {
            sendEmailNotification(notification);
        }
    }
    
    private void sendEmailNotification(Notification notification) {
        // Get the current user's email
        String userEmail = getCurrentUserEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            System.err.println("Cannot send email notification: User email not found");
            return;
        }
        
        // Send email in background thread
        emailExecutor.submit(() -> {
            try {
                emailService.sendNotificationEmail(
                    userEmail,
                    notification.getTitle(),
                    notification.getMessage()
                );
                System.out.println("Email notification sent to: " + userEmail);
            } catch (jakarta.mail.MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private String getCurrentUserEmail() {
        // Get current user identifier using RememberMeHandler's static method
        String identifier = RememberMeHandler.getCurrentUserIdentifier();
        if (identifier == null || identifier.isEmpty()) {
            System.err.println("NotificationManager: User identifier not found.");
            return null;
        }
        
        // Otherwise, query the database for the email from students or faculty table
        String email = null;
        // Try students table
        String studentQuery = "SELECT email FROM students WHERE student_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(studentQuery)) {
            statement.setString(1, identifier);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                email = result.getString("email");
                if (email != null && !email.isEmpty()) return email;
            }
        } catch (SQLException e) {
            System.err.println("NotificationManager: Error querying students table for email: " + e.getMessage());
        }
        
        // Try faculty table if not found in students
        String facultyQuery = "SELECT email FROM faculty WHERE faculty_number = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(facultyQuery)) {
            statement.setString(1, identifier);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                email = result.getString("email");
            }
        } catch (SQLException e) {
            System.err.println("NotificationManager: Error querying faculty table for email: " + e.getMessage());
        }

        if (email == null) {
            System.err.println("NotificationManager: Email not found in DB for identifier: " + identifier);
        }
        return email;
    }
    
    private void showEmailErrorPopup(String errorMessage) {
        Stage owner = getActiveStage();
        if (owner == null) return;
        
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initOwner(owner);
        dialog.setTitle("Email Error");
        
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 20; -fx-background-color: white;");
        
        Label messageLabel = new Label("Failed to send email notification:\n" + errorMessage);
        messageLabel.setWrapText(true);
        
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        
        content.getChildren().addAll(messageLabel, closeButton);
        
        Scene scene = new Scene(content, 400, 200);
        dialog.setScene(scene);
        dialog.show();
    }
    
    private Stage getActiveStage() {
        // Try to find the currently active stage
        for (Stage stage : getStages()) {
            if (stage.isFocused()) {
                return stage;
            }
        }
        
        // If no focused stage is found, return the first one
        List<Stage> stages = getStages();
        return stages.isEmpty() ? null : stages.get(0);
    }
    
    private List<Stage> getStages() {
        // This is a workaround to get all stages
        List<Stage> stages = new ArrayList<>();
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage) {
                stages.add((Stage) window);
            }
        }
        return stages;
    }
    
    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications);
    }
    
    public void clearNotifications() {
        notifications.clear();
    }
    
    public enum NotificationType {
        EMAIL, GRADE, ANNOUNCEMENT, SYSTEM
    }
    
    public static class Notification {
        private final String title;
        private final String message;
        private final NotificationType type;
        private final long timestamp;
        
        public Notification(String title, String message, NotificationType type) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getMessage() {
            return message;
        }
        
        public NotificationType getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
