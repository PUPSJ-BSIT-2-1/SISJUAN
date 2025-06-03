package com.example.pupsis_main_dashboard.utilities;

import java.util.prefs.Preferences;

public class RememberMeHandler {

    private static final String LAST_USERNAME_PREFIX = "last_username_";
    private static final String REMEMBER_ME_SELECTED_PREFIX = "remember_me_selected_";

    // For current session email (global)
    private static final String KEY_CURRENT_SESSION_EMAIL = "current_session_email";
    private static String currentSessionEmail = null;

    private static final Preferences preferences = Preferences.userNodeForPackage(RememberMeHandler.class);

    /**
     * Saves the "Remember Me" preference for a specific user type.
     * It always saves the username as the last used username for that type.
     *
     * @param userType   A string identifying the user type (e.g., "ADMIN", "STUDENT", "FACULTY").
     * @param username   The username or ID.
     * @param rememberMe True if the "Remember Me" checkbox was selected.
     */
    public static void savePreference(String userType, String username, boolean rememberMe) {
        String lastUsernameKey = LAST_USERNAME_PREFIX + userType;
        String rememberMeSelectedKey = REMEMBER_ME_SELECTED_PREFIX + userType;

        if (username != null && !username.isEmpty()) {
            preferences.put(lastUsernameKey, username); // Always save/update last used username
            preferences.putBoolean(rememberMeSelectedKey, rememberMe);
        } else {
            // If username is null or empty, clear preferences for this type
            clearUserTypePreferences(userType);
        }
    }

    /**
     * Loads the last used username for a specific user type.
     *
     * @param userType A string identifying the user type.
     * @return The last used username, or an empty string if none was saved.
     */
    public static String getLastUsedUsername(String userType) {
        String lastUsernameKey = LAST_USERNAME_PREFIX + userType;
        return preferences.get(lastUsernameKey, ""); // Return empty string if not found
    }

    /**
     * Checks if "Remember Me" was selected for the given user type during the last session
     * where preferences were saved for a non-empty username.
     *
     * @param userType A string identifying the user type.
     * @return True if "Remember Me" was selected for a stored username, false otherwise.
     */
    public static boolean wasRememberMeSelected(String userType) {
        String lastUsername = getLastUsedUsername(userType);
        if (!lastUsername.isEmpty()) {
            String rememberMeSelectedKey = REMEMBER_ME_SELECTED_PREFIX + userType;
            return preferences.getBoolean(rememberMeSelectedKey, false);
        }
        return false; // No last username, so "remember me" couldn't have been selected for it.
    }

    /**
     * Clears all "Remember Me" related preferences for a specific user type.
     *
     * @param userType The user type whose preferences should be cleared.
     */
    public static void clearUserTypePreferences(String userType) {
        String lastUsernameKey = LAST_USERNAME_PREFIX + userType;
        String rememberMeSelectedKey = REMEMBER_ME_SELECTED_PREFIX + userType;
        preferences.remove(lastUsernameKey);
        preferences.remove(rememberMeSelectedKey);
    }


    // --- Current Session Email Management (kept as is from original, for global session tracking) ---
    public static String getCurrentUserEmail() {
        if (currentSessionEmail != null && !currentSessionEmail.isEmpty()) {
            return currentSessionEmail;
        }
        String prefEmail = preferences.get(KEY_CURRENT_SESSION_EMAIL, null);
        if (prefEmail != null && !prefEmail.isEmpty()) {
            currentSessionEmail = prefEmail;
            return prefEmail;
        }
        return null;
    }

    public static void setCurrentUserEmail(String email) {
        currentSessionEmail = email;
        if (email != null && !email.isEmpty()) {
            preferences.put(KEY_CURRENT_SESSION_EMAIL, email);
        } else {
            preferences.remove(KEY_CURRENT_SESSION_EMAIL);
        }
    }

    public static void clearCurrentSessionEmail() {
        currentSessionEmail = null;
        preferences.remove(KEY_CURRENT_SESSION_EMAIL);
    }

    /**
     * Actions on logout, primarily clearing the active session email.
     * "Remember Me" preferences persist based on user selection.
     */
    public static void onLogout() {
        clearCurrentSessionEmail();
    }
}