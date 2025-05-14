package com.example.pupsis_main_dashboard.controllers;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;

import java.util.Objects;

public class AboutContentController {

    @FXML private VBox root;
    @FXML private ComboBox<String> modulePicker;
    @FXML private ImageView previous;
    @FXML private ImageView next;
    @FXML private ImageView image;
    @FXML private Label name;
    @FXML private Label role;
    @FXML private Label description;

    @FXML
    private void initialize() {
        populateModule();

        // Wait until the scene is available to check the theme
        root.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                // Initial theme setup
                updateIconsBasedOnTheme();

                // Listen for theme changes on the scene's root
                newScene.getRoot().getStyleClass().addListener((ListChangeListener<String>) _ ->
                        updateIconsBasedOnTheme());
            }
        });
    }

    private void populateModule() {
        String[] modules = {
                "Main Dashboard",
                "Registration",
                "Payment Information",
                "Room Assignment",
                "Grading System",
                "Class Schedule",
                "Faculty"
        };
        modulePicker.getItems().addAll(modules);
    }

    private void updateIconsBasedOnTheme() {
        // Get the root of the scene which should have the theme class
        Parent sceneRoot = root.getScene() != null ? root.getScene().getRoot() : null;
        if (sceneRoot == null) return;

        boolean isDark = sceneRoot.getStyleClass().contains("dark-theme");

        String prevImagePath = isDark
                ? "/com/example/pupsis_main_dashboard/Images/previous-white.png"
                : "/com/example/pupsis_main_dashboard/Images/previous.png";

        String nextImagePath = isDark
                ? "/com/example/pupsis_main_dashboard/Images/next-white.png"
                : "/com/example/pupsis_main_dashboard/Images/next.png";

        try {
            Image prevImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(prevImagePath)));
            Image nextImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(nextImagePath)));



            previous.setImage(prevImage);
            next.setImage(nextImage);

            if (isDark) {
                if (!previous.getStyleClass().contains("dark-buttons")) {
                    previous.getStyleClass().add("dark-buttons");
                }
                if (!next.getStyleClass().contains("dark-buttons")) {
                    next.getStyleClass().add("dark-buttons");
                }
            } else {
                previous.getStyleClass().remove("dark-buttons");
                next.getStyleClass().remove("dark-buttons");
            }

        } catch (NullPointerException e) {
            System.err.println("Failed to load image resources: " + e.getMessage());
        }
    }
}