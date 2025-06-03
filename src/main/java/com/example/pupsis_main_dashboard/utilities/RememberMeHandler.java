package com.example.pupsis_main_dashboard.utilities;

import java.util.prefs.Preferences;

public class RememberMeHandler {

    private static final Preferences prefs = Preferences.userNodeForPackage(RememberMeHandler.class);
    private static final String LAST_USERNAME_KEY_SUFFIX = "_last_username";
    private static final String REMEMBER_ME_KEY_SUFFIX = "_remember_me_selected";
    private static final String PASSWORD_KEY_SUFFIX = "_password"; // Key for storing password
    private static final String CURRENT_SESSION_EMAIL_KEY = "current_session_email";

    // Save preferences for a specific user type
    public static void savePreference(String userType, String username, String password, boolean rememberMe) {
        if (rememberMe) {
            prefs.put(userType + LAST_USERNAME_KEY_SUFFIX, username);
            prefs.put(userType + PASSWORD_KEY_SUFFIX, password); // Save password
            prefs.putBoolean(userType + REMEMBER_ME_KEY_SUFFIX, true);
        } else {
            // If remember me is not selected, clear the specific preference for that user type
            // but keep the last username for convenience if they re-check next time.
            prefs.put(userType + LAST_USERNAME_KEY_SUFFIX, username); // Still save username as last used
            prefs.remove(userType + PASSWORD_KEY_SUFFIX); // Remove password
            prefs.putBoolean(userType + REMEMBER_ME_KEY_SUFFIX, false);
        }
    }

    // Get the last used username for a specific user type
    public static String getLastUsedUsername(String userType) {
        return prefs.get(userType + LAST_USERNAME_KEY_SUFFIX, null);
    }

    // Get the saved password for a specific user type
    public static String getSavedPassword(String userType) {
        // Only return password if remember me was actually selected
        if (wasRememberMeSelected(userType)) {
            return prefs.get(userType + PASSWORD_KEY_SUFFIX, null);
        }
        return null;
    }

    // Check if "Remember Me" was selected for a specific user type
    public static boolean wasRememberMeSelected(String userType) {
        return prefs.getBoolean(userType + REMEMBER_ME_KEY_SUFFIX, false);
    }

    // Clear all preferences for a specific user type (e.g., on explicit logout or account action)
    public static void clearUserTypePreferences(String userType) {
        prefs.remove(userType + LAST_USERNAME_KEY_SUFFIX);
        prefs.remove(userType + REMEMBER_ME_KEY_SUFFIX);
        prefs.remove(userType + PASSWORD_KEY_SUFFIX); // Clear saved password
    }

    // --- Current Session Email Management ---
    // (These methods remain global, not user-type specific for session tracking)

    public static String getCurrentUserEmail() {
        return prefs.get(CURRENT_SESSION_EMAIL_KEY, null);
    }

    public static void setCurrentUserEmail(String email) {
        if (email != null && !email.isEmpty()) {
            prefs.put(CURRENT_SESSION_EMAIL_KEY, email);
        } else {
            prefs.remove(CURRENT_SESSION_EMAIL_KEY);
        }
    }

    public static void clearCurrentSessionEmail() {
        prefs.remove(CURRENT_SESSION_EMAIL_KEY);
    }

    /**
     * Call this method when a user logs out to clear their session email
     * and optionally their specific user-type preferences if remember me was not selected.
     * If remember me was selected for their user type, their username (and now password) would persist.
     */
    public static void onLogout(String userType) {
        clearCurrentSessionEmail();
        // Optionally, if you want to clear even "remembered" details on logout, call:
        // clearUserTypePreferences(userType);
        // However, typical "Remember Me" implies details persist across sessions until explicitly cleared
        // or when "Remember Me" is unchecked during a login.
    }
}