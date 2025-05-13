/**
 * Main application class for PUPSIS.
 * This class initializes the JavaFX application and loads the main dashboard.
 */

package com.example.pupsis_main_dashboard;

import com.example.pupsis_main_dashboard.controllers.SettingsController; 
import com.example.pupsis_main_dashboard.utility.StageAndSceneUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class PUPSIS extends Application {
    private static final Logger logger = LoggerFactory.getLogger(PUPSIS.class);
    private static final String DARK_MODE_CSS_PATH = "/com/example/pupsis_main_dashboard/css/DarkMode.css";
    private static final String DARK_THEME_CLASS = "dark-theme";
    private static final String LIGHT_THEME_CLASS = "light-theme";

    private static void applyThemeToSingleScene(Scene scene, boolean darkModeEnabled) {
        if (scene == null) {
            logger.warn("Cannot apply theme: scene is null.");
            return;
        }
        javafx.scene.Node sceneRoot = scene.getRoot();
        if (sceneRoot == null) {
            logger.warn("Cannot apply theme: scene root is null for scene {}", scene);
        }

        String darkThemeCssUrl = null;
        try {
            darkThemeCssUrl = Objects.requireNonNull(PUPSIS.class.getResource(DARK_MODE_CSS_PATH)).toExternalForm();
        } catch (NullPointerException e) {
            logger.error("Error: Cannot load dark theme CSS in PUPSIS: {}. CSS file may be missing or path incorrect.", DARK_MODE_CSS_PATH, e);
        }

        if (darkModeEnabled) {
            if (darkThemeCssUrl != null && !scene.getStylesheets().contains(darkThemeCssUrl)) {
                scene.getStylesheets().add(darkThemeCssUrl);
                logger.debug("Applied dark mode stylesheet to scene {}.");
            }
            if (sceneRoot != null) {
                if (!sceneRoot.getStyleClass().contains(DARK_THEME_CLASS)) {
                    sceneRoot.getStyleClass().add(DARK_THEME_CLASS);
                }
                sceneRoot.getStyleClass().remove(LIGHT_THEME_CLASS);
                logger.debug("Applied '{}' class and removed '{}' class from scene root for scene {}.", DARK_THEME_CLASS, LIGHT_THEME_CLASS);
            }
        } else {
            if (darkThemeCssUrl != null && scene.getStylesheets().contains(darkThemeCssUrl)) {
                scene.getStylesheets().remove(darkThemeCssUrl);
                logger.debug("Removed dark mode stylesheet from scene {}.");
            }
            if (sceneRoot != null) {
                if (!sceneRoot.getStyleClass().contains(LIGHT_THEME_CLASS)) {
                    sceneRoot.getStyleClass().add(LIGHT_THEME_CLASS);
                }
                sceneRoot.getStyleClass().remove(DARK_THEME_CLASS);
                logger.debug("Applied '{}' class and removed '{}' class from scene root for scene {}.", LIGHT_THEME_CLASS, DARK_THEME_CLASS);
            }
        }
        logger.debug("applyThemeToSingleScene finished for scene {} with darkModeEnabled: {}", scene.hashCode(), darkModeEnabled);
        if (sceneRoot != null) {
             logger.debug("Scene root style classes for scene {}: {}", scene.hashCode(), sceneRoot.getStyleClass());
        }
    }

    public static void applyGlobalTheme(Scene scene) {
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean darkModeEnabled = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
        logger.debug("applyGlobalTheme (for new scene) called. darkModeEnabled preference: {}", darkModeEnabled);
        applyThemeToSingleScene(scene, darkModeEnabled);
    }

    public static void triggerGlobalThemeUpdate() {
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean darkModeEnabled = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
        logger.info("triggerGlobalThemeUpdate called. Applying darkModeEnabled: {} to all windows.", darkModeEnabled);

        for (Window window : Window.getWindows()) {
            if (window instanceof Stage) {
                Scene scene = ((Stage) window).getScene();
                if (scene != null) {
                    applyThemeToSingleScene(scene, darkModeEnabled);
                } else {
                    logger.debug("Window {} is a Stage but has no scene, skipping theme update.", window);
                }
            } else {
                 logger.debug("Window {} is not a Stage, skipping theme update.", window);
            }
        }
        logger.info("triggerGlobalThemeUpdate finished.");
    }

    @Override
    public void start(Stage stage) {
        StageAndSceneUtils utility = new StageAndSceneUtils();

        try {
            Stage initializedStage = utility.loadStage(
                    "fxml/FrontPage.fxml",
                    "PUPSIS",
                    Objects.requireNonNull(getClass().getResource("/com/example/pupsis_main_dashboard/Images/PUPSJ Logo.png")).toExternalForm(),
                    StageAndSceneUtils.WindowSize.MEDIUM
            );

            Scene scene = initializedStage.getScene();
            applyGlobalTheme(scene);

            initializedStage.show();
        } catch (IOException e) {
            logger.error("Error initializing stage", e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}