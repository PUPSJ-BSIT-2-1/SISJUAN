package com.example.pupsis_main_dashboard;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class HelloController {
    @FXML
    private VBox sidebar;
    private boolean isSidebarVisible = true;
    @FXML
    Label currentDate;

    @FXML
    private void initialize() {
        currentDate.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")));
    }
    @FXML
    private void toggleSidebar() {
        // Sidebar sliding animation
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), sidebar);

        if (isSidebarVisible) {
            // Slide the sidebar out (hidden)
            transition.setToX(-sidebar.getWidth());
            transition.setOnFinished(event -> sidebar.setVisible(false)); // Hide the sidebar after the animation
        } else {
            // Slide the sidebar in (visible)
            sidebar.setVisible(true); // Make it visible before sliding in
            transition.setToX(0);
        }

        // Toggle the sidebar visibility state
        isSidebarVisible = !isSidebarVisible;
        transition.play();
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
    private void closeProgram() {
        Platform.exit();
    }
}