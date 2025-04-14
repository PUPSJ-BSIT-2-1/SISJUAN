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
    private void initialize() {
        currentDate.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")));
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
}