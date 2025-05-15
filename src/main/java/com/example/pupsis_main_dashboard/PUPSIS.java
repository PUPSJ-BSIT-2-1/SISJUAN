/**
 * Main application class for PUPSIS.
 * This class initializes the JavaFX application and loads the main dashboard.
 */

package com.example.pupsis_main_dashboard;

import com.example.pupsis_main_dashboard.controllers.SettingsController; 
import com.example.pupsis_main_dashboard.utilities.StageAndSceneUtils;
import javafx.application.Application;
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
            return;
        }
        javafx.scene.Node sceneRoot = scene.getRoot();
        if (sceneRoot == null) {
            return;
        }

        String darkThemeCssUrl = null;
        try {
            darkThemeCssUrl = Objects.requireNonNull(PUPSIS.class.getResource(DARK_MODE_CSS_PATH)).toExternalForm();
        } catch (NullPointerException e) {
            // Cannot load dark theme CSS
        }

        if (darkModeEnabled) {
            if (darkThemeCssUrl != null && !scene.getStylesheets().contains(darkThemeCssUrl)) {
                scene.getStylesheets().add(darkThemeCssUrl);
            }
            if (sceneRoot != null) {
                if (!sceneRoot.getStyleClass().contains(DARK_THEME_CLASS)) {
                    sceneRoot.getStyleClass().add(DARK_THEME_CLASS);
                }
                sceneRoot.getStyleClass().remove(LIGHT_THEME_CLASS);
            }
        } else {
            if (darkThemeCssUrl != null && scene.getStylesheets().contains(darkThemeCssUrl)) {
                scene.getStylesheets().remove(darkThemeCssUrl);
            }
            if (sceneRoot != null) {
                if (!sceneRoot.getStyleClass().contains(LIGHT_THEME_CLASS)) {
                    sceneRoot.getStyleClass().add(LIGHT_THEME_CLASS);
                }
                sceneRoot.getStyleClass().remove(DARK_THEME_CLASS);
            }
        }
    }

    public static void applyGlobalTheme(Scene scene) {
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean darkModeEnabled = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);
        applyThemeToSingleScene(scene, darkModeEnabled);
    }

    public static void triggerGlobalThemeUpdate() {
        Preferences settingsPrefs = Preferences.userNodeForPackage(SettingsController.class);
        boolean darkModeEnabled = settingsPrefs.getBoolean(SettingsController.THEME_PREF, false);

        for (Window window : Window.getWindows()) {
            if (window instanceof Stage) {
                Scene scene = ((Stage) window).getScene();
                if (scene != null) {
                    applyThemeToSingleScene(scene, darkModeEnabled);
                }
            }
        }
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