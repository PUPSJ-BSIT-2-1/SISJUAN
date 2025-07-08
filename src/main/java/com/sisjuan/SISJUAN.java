/**
 * Main application class for SISJUAN.
 * This class initializes the JavaFX application and loads the main dashboard.
 */

package com.sisjuan;

import com.sisjuan.controllers.GeneralSettingsController;
import com.sisjuan.utilities.StageAndSceneUtils;
import com.sisjuan.utilities.RememberMeHandler;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class SISJUAN extends Application {
    private static final Logger logger = LoggerFactory.getLogger(SISJUAN.class);
    private static final String DARK_MODE_CSS_PATH = "/com/sisjuan/css/GeneralDarkMode.css";
    private static final String DARK_THEME_CLASS = "dark-theme";
    private static final String LIGHT_THEME_CLASS = "light-theme";

    public static void applyThemeToSingleScene(Scene scene, boolean darkModeEnabled) {
        if (scene == null) {
            return;
        }
        javafx.scene.Node sceneRoot = scene.getRoot();
        if (sceneRoot == null) {
            return;
        }

        String darkThemeCssUrl = null;
        try {
            darkThemeCssUrl = Objects.requireNonNull(SISJUAN.class.getResource(DARK_MODE_CSS_PATH)).toExternalForm();
        } catch (NullPointerException e) {
            logger.error("Could not load dark theme CSS", e);
        }

        if (darkModeEnabled) {
            // Apply dark theme CSS stylesheet if not already added
            if (darkThemeCssUrl != null && !scene.getStylesheets().contains(darkThemeCssUrl)) {
                scene.getStylesheets().add(darkThemeCssUrl);
            }
            
            // Apply dark theme class to root
            if (!sceneRoot.getStyleClass().contains(DARK_THEME_CLASS)) {
                sceneRoot.getStyleClass().add(DARK_THEME_CLASS);
            }
            sceneRoot.getStyleClass().remove(LIGHT_THEME_CLASS);
        } else {
            if (darkThemeCssUrl != null && scene.getStylesheets().contains(darkThemeCssUrl)) {
                scene.getStylesheets().remove(darkThemeCssUrl);
            }
            
            // Apply light theme class to root
            if (!sceneRoot.getStyleClass().contains(LIGHT_THEME_CLASS)) {
                sceneRoot.getStyleClass().add(LIGHT_THEME_CLASS);
            }
            sceneRoot.getStyleClass().remove(DARK_THEME_CLASS);
        }
    }

    public static void applyGlobalTheme(Scene scene) {
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();
        boolean darkModeEnabled = false; // Default to light theme
        if (currentUserIdentifier != null && !currentUserIdentifier.isEmpty()) {
            String userType = RememberMeHandler.getUserTypeFromIdentifier(currentUserIdentifier);
            if (userType != null && !userType.equals("UNKNOWN") && !userType.isEmpty()) {
                Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(userType.toUpperCase());
                darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
            } else {
                logger.warn("applyGlobalTheme: Could not determine user type for identifier '{}'. Defaulting to light theme.", currentUserIdentifier);
            }
        } else {
            // No current user identified (e.g. on FrontPage), default to light theme.
            // logger.info("applyGlobalTheme: No current user identifier. Defaulting to light theme.");
        }
        applyThemeToSingleScene(scene, darkModeEnabled);
    }

    public static void triggerGlobalThemeUpdate() {
        String currentUserIdentifier = RememberMeHandler.getCurrentUserIdentifier();
        boolean darkModeEnabled = false; // Default to light theme
        if (currentUserIdentifier != null && !currentUserIdentifier.isEmpty()) {
            String userType = RememberMeHandler.getUserTypeFromIdentifier(currentUserIdentifier);
            if (userType != null && !userType.equals("UNKNOWN") && !userType.isEmpty()) {
                Preferences userPrefs = Preferences.userNodeForPackage(GeneralSettingsController.class).node(userType.toUpperCase());
                darkModeEnabled = userPrefs.getBoolean(GeneralSettingsController.THEME_PREF, false);
            } else {
                 logger.warn("triggerGlobalThemeUpdate: Could not determine user type for identifier '{}'. Defaulting to light theme for update.", currentUserIdentifier);
            }
        } else {
             // No current user, perhaps this update should be skipped or use a general default.
             // logger.info("triggerGlobalThemeUpdate: No current user identifier. Defaulting to light theme for update.");
        }

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
                    "fxml/GeneralFrontPage.fxml",
                    "SISJUAN",
                    Objects.requireNonNull(getClass().getResource("/com/sisjuan/Images/PUPSJ Logo.png")).toExternalForm(),
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