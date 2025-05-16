package com.example.pupsis_main_dashboard.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AboutContentController {

    @FXML private VBox root;
    @FXML private ComboBox<String> modulePicker;
    @FXML private ImageView previous;
    @FXML private ImageView next;
    @FXML private ImageView image;
    @FXML private Label name;
    @FXML private Label role;
    @FXML private Label description;

    private List<Developer> developers;
    private List<Developer> filteredDevelopers = new ArrayList<>();
    private int currentIndex = 0;

    // Class to represent developer information
    public static class Developer {
        private String devName;
        private String devRole;
        private String devDesc;
        private String devImage;
        private String devModule;

        @SuppressWarnings("unused")
        public Developer(String devName, String devRole, String devDesc, String devImage, String devModule) {
            this.devName = devName;
            this.devRole = devRole;
            this.devDesc = devDesc;
            this.devImage = devImage;
            this.devModule = devModule;
        }

        public Developer() {}

        public String getDevName() {
            return devName;
        }

        public String getDevRole() {
            return devRole;
        }

        public String getDevDesc() {
            return devDesc;
        }

        public String getDevImage() {
            return devImage;
        }

        public String getDevModule() {
            return devModule;
        }
    }

    // Initializes the controller by populating the module picker and loading developer content.
    @FXML private void initialize() {
        populateModule();
        loadDevelopersContent();
        if (!modulePicker.getItems().isEmpty()) {
            modulePicker.getSelectionModel().selectFirst();  // Select the first module initially
            String selectedModule = modulePicker.getValue();
            filteredDevelopers = developers.stream()
                    .filter(dev -> dev.getDevModule().equalsIgnoreCase(selectedModule))
                    .collect(Collectors.toList());
            showDeveloperDetails(currentIndex);
        }
        handleModuleSelection();
        handleButtons();

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

    // Loads developer information from a JSON file.
    private void loadDevelopersContent() {
        final String devPath = "/com/example/pupsis_main_dashboard/json/DevelopersTeam.json";
        try (InputStream inputStream = getClass().getResourceAsStream(devPath)) {
            ObjectMapper objectMapper = new ObjectMapper();
            developers = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handles the selection of modules from the combo box.
    private void handleModuleSelection() {
        modulePicker.setOnAction(_ -> {
            currentIndex = 0;
            String selectedModule = modulePicker.getValue();
            filteredDevelopers = developers.stream()
                    .filter(dev -> dev.getDevModule().equalsIgnoreCase(selectedModule))
                    .collect(Collectors.toList());
            showDeveloperDetails(currentIndex);
        });
    }

    // Handles the next and previous buttons for navigating through developers.
    private void handleButtons() {
        next.setOnMouseClicked(_ -> {
            if (!filteredDevelopers.isEmpty()) {
                currentIndex = (currentIndex + 1) % filteredDevelopers.size();
                showDeveloperDetails(currentIndex);
            }
        });

        previous.setOnMouseClicked(_ -> {
            if (!filteredDevelopers.isEmpty()) {
                currentIndex = (currentIndex - 1 + filteredDevelopers.size()) % filteredDevelopers.size();
                showDeveloperDetails(currentIndex);
            }
        });
    }

    // Displays the details of the developer at the specified index.
    private void showDeveloperDetails(int index) {
        if (index >= 0 && index < filteredDevelopers.size()) {
            Developer dev = filteredDevelopers.get(index);
            name.setText(dev.getDevName());
            role.setText(dev.getDevRole());
            description.setText(dev.getDevDesc());
            try {
                Image devImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/example/pupsis_main_dashboard/" + dev.getDevImage())));
                image.setPreserveRatio(false);
                image.setFitWidth(200);
                image.setFitHeight(200);
                image.setImage(devImage);
            } catch (NullPointerException e) {
                System.err.println("Failed to load image resources: " + e.getMessage());
            }
        }
    }

    // Populates the module picker with available modules.
    private void populateModule() {
        String[] modules = {
                "Main Dashboard",
                "Registration",
                "Payment",
                "Room Assignment",
                "Grading",
                "Class Schedule",
                "Faculty"
        };
        modulePicker.getItems().addAll(modules);
    }

    // Updates the icons based on the current theme (dark or light).
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