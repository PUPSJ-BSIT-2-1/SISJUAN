/**
 * Utility class for handling "Remember Me" functionality.
 * This class provides methods to save, load, and clear user credentials.
 */

package com.example.pupsis_main_dashboard.utilities;

import java.util.prefs.Preferences;

public class RememberMeHandler {
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_CURRENT_SESSION_EMAIL = "current_session_email";

    private static final Preferences preferences = Preferences.userNodeForPackage(RememberMeHandler.class);
    
    // Store the current user's email for the active session
    private static String currentSessionEmail = null;

    public static void saveCredentials(String username, String password, boolean rememberMe) {
        // Always store the current session email
        currentSessionEmail = username;
        
        if (rememberMe) {
            preferences.putBoolean(KEY_REMEMBER_ME, true);
            preferences.put(KEY_USERNAME, username);
            preferences.put(KEY_PASSWORD, encrypt(password));
            preferences.put(KEY_CURRENT_SESSION_EMAIL, username);
        } else {
            clearRememberedCredentials();
            // Still save the current session email
            preferences.put(KEY_CURRENT_SESSION_EMAIL, username);
        }
    }

    public static String[] loadCredentials() {
        if (preferences.getBoolean(KEY_REMEMBER_ME, false)) {
            String username = preferences.get(KEY_USERNAME, "");
            String password = decrypt(preferences.get(KEY_PASSWORD, ""));
            return new String[]{username, password};
        }
        return null;
    }
    
    /**
     * Gets the email of the currently logged-in user, regardless of remember me status.
     * @return The email of the current user or null if not logged in
     */
    public static String getCurrentUserEmail() {
        // First try to get from memory
        if (currentSessionEmail != null && !currentSessionEmail.isEmpty()) {
            return currentSessionEmail;
        }
        
        // If not in memory, try to get from preferences
        String prefEmail = preferences.get(KEY_CURRENT_SESSION_EMAIL, null);
        if (prefEmail != null && !prefEmail.isEmpty()) {
            currentSessionEmail = prefEmail; // Cache it in memory
            return prefEmail;
        }
        
        // Last resort: try to get from remembered credentials
        String[] credentials = loadCredentials();
        if (credentials != null) {
            currentSessionEmail = credentials[0]; // Cache it in memory
            return credentials[0];
        }
        
        return null;
    }
    
    /**
     * Sets the email for the current user session
     * @param email The email to set for the current session
     */
    public static void setCurrentUserEmail(String email) {
        currentSessionEmail = email;
        preferences.put(KEY_CURRENT_SESSION_EMAIL, email);
    }

    public static void clearCredentials() {
        clearRememberedCredentials();
        clearCurrentSession();
    }
    
    private static void clearRememberedCredentials() {
        preferences.remove(KEY_REMEMBER_ME);
        preferences.remove(KEY_USERNAME);
        preferences.remove(KEY_PASSWORD);
    }
    
    private static void clearCurrentSession() {
        currentSessionEmail = null;
        preferences.remove(KEY_CURRENT_SESSION_EMAIL);
    }

    private static String encrypt(String data) {
        return data;
    }

    private static String decrypt(String data) {
        return data;
    }
}