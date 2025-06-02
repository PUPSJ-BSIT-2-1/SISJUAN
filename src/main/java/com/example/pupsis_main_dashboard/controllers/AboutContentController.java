package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Developer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AboutContentController extends Developer {

    @FXML private VBox root;
    @FXML private ComboBox<String> modulePicker;
    @FXML private ImageView previous;
    @FXML private ImageView next;
    @FXML private ImageView image;
    @FXML private Label name;
    @FXML private Label role;
    @FXML private Label description;
    @FXML private StackPane stackPane;

    private List<Developer> developers;
    private List<Developer> filteredDevelopers = new ArrayList<>();
    private int currentIndex = 0;

    private final Logger logger = LoggerFactory.getLogger(AboutContentController.class);

    /**
     * Initializes the controller by populating the module picker, loading developer content,
     * and setting up the initial UI state. It selects the first module by default and filters
     * the developers based on the selected module.
     */
    @FXML private void initialize() {
        populateModule();
        loadDevelopersContent();
        if (!modulePicker.getItems().isEmpty()) {
            modulePicker.getSelectionModel().selectFirst();
            String selectedModule = modulePicker.getValue();
            filteredDevelopers = developers.stream()
                    .filter(dev -> dev.getDevModule().equalsIgnoreCase(selectedModule))
                    .collect(Collectors.toList());
            showDeveloperDetails(currentIndex);
        }
        handleModuleSelection();
        handleButtons();

        root.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null) {
                updateIconsBasedOnTheme();
                newScene.getRoot().getStyleClass().addListener((ListChangeListener<String>) _ ->
                        updateIconsBasedOnTheme());
            }
        });
    }

    // Loads the developer content from a JSON file
    private void loadDevelopersContent() {
        final String devPath = "/com/example/pupsis_main_dashboard/json/DevelopersTeam.json";
        try (InputStream inputStream = getClass().getResourceAsStream(devPath)) {
            ObjectMapper objectMapper = new ObjectMapper();
            developers = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            logger.error("Failed to load developers content: {}", e.getMessage());
        }
    }

    // Populates the module picker with unique module names
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

    // Handles the next and previous buttons
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

    // Shows the details of the developer at the specified index
    private void showDeveloperDetails(int index) {
        if (index >= 0 && index < filteredDevelopers.size()) {
            Developer dev = filteredDevelopers.get(index);

            // Apply fade transition animation
            applyFadeTransition(name, dev.getDevName());
            applyFadeTransition(role, dev.getDevRole());
            applyFadeTransition(description, dev.getDevDesc());

            try {
                Image devImage = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/com/example/pupsis_main_dashboard/" + dev.getDevImage())
                ));
                image.setImage(devImage);
                image.setFitWidth(200);
                image.setFitHeight(200);
                image.setPreserveRatio(false);

                stackPane.getChildren().setAll(image, getRectangle());
                stackPane.setPrefSize(200, 200);

                Rectangle clip = new Rectangle(200, 200);
                clip.setArcWidth(30);
                clip.setArcHeight(30);
                image.setClip(clip);

                FadeTransition fadeTransitionOut = new FadeTransition(Duration.millis(400), image);
                fadeTransitionOut.setFromValue(1.0);
                fadeTransitionOut.setToValue(0.0);
                fadeTransitionOut.play();
                fadeTransitionOut.setOnFinished(_ -> {
                    FadeTransition fadeTransitionIn = new FadeTransition(Duration.millis(400), image);
                    fadeTransitionIn.setFromValue(0.0);
                    fadeTransitionIn.setToValue(1.0);
                    fadeTransitionIn.play();
                });
                fadeTransitionOut.play();

            } catch (NullPointerException e) {
                System.err.println("Failed to load image resources: " + e.getMessage());
            }
        }
    }

    // Applies a fade transition animation to a label
    private void applyFadeTransition(Label label, String newText) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(_ -> {
            label.setText(newText);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), label);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // Creates a gradient background for the image
    private Rectangle getRectangle() {
        Rectangle gradientBackground = new Rectangle(200, 200);
        LinearGradient gradient = new LinearGradient(
                0, 1, 0, 0,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(128, 0, 0, 0.9)), // Faded maroon start
                new Stop(0.6, Color.TRANSPARENT)
        );
        gradientBackground.setFill(gradient);
        gradientBackground.setArcWidth(20);
        gradientBackground.setArcHeight(20);

        FadeTransition fadeTransitionOut = new FadeTransition(Duration.millis(400), gradientBackground);
        fadeTransitionOut.setFromValue(1.0);
        fadeTransitionOut.setToValue(0.0);
        fadeTransitionOut.play();
        fadeTransitionOut.setOnFinished(_ -> {
            FadeTransition fadeTransitionIn = new FadeTransition(Duration.millis(400), gradientBackground);
            fadeTransitionIn.setFromValue(0.0);
            fadeTransitionIn.setToValue(1.0);
            fadeTransitionIn.play();
        });
        fadeTransitionOut.play();
        return gradientBackground;
    }

    // Populates the module picker with unique module names
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

    // Updates the icons based on the current theme
    private void updateIconsBasedOnTheme() {
        Parent sceneRoot = root.getScene() != null ? root.getScene().getRoot() : null;
        if (sceneRoot == null) return;

        boolean isDark = sceneRoot.getStyleClass().contains("dark-theme");

        String prevImagePath = isDark
                ? "/com/example/pupsis_main_dashboard/Images/previous-black.png"
                : "/com/example/pupsis_main_dashboard/Images/previous.png";

        String nextImagePath = isDark
                ? "/com/example/pupsis_main_dashboard/Images/next-black.png"
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