package com.example.pupsis_main_dashboard;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class StudentMainDashoardController {
    @FXML
    private VBox sidebar;
    private boolean isSidebarVisible = true;
    @FXML
    private Label currentDate;
    @FXML
    private Label welcomeLabel;

    @FXML
    private void initialize() {
        StudentLoginPageController loginPageController = new StudentLoginPageController();
        currentDate.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")));
//        welcomeLabel.setText("Welcome, " + loginPageController.getUsername());
    }

    @FXML
    private void handleSidebarItemClick(MouseEvent event) {
        // Get the clicked sidebar item (assuming it's a container like HBox)
        Node clickedNode = (Node) event.getSource();

        // Reset styles for all sidebar items
        sidebar.getChildren().forEach(node -> node.getStyleClass().remove("selected-item"));

        // Apply "selected-item" style to the clicked item
        clickedNode.getStyleClass().add("selected-item");
    }

    @FXML
    private void LogoutButton(MouseEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("RolePick.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen(); // Center the stage on the screen
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}