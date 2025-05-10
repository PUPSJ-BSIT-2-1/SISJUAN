/**
 * Utility class for handling "Remember Me" functionality.
 * This class provides methods to save, load, and clear user credentials.
 */

package com.example.pupsis_main_dashboard.utility;

import java.util.prefs.Preferences;

public class RememberMeHandler {
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private static final Preferences preferences = Preferences.userNodeForPackage(RememberMeHandler.class);

    public static void saveCredentials(String username, String password, boolean rememberMe) {
        if (rememberMe) {
            preferences.putBoolean(KEY_REMEMBER_ME, true);
            preferences.put(KEY_USERNAME, username);
            preferences.put(KEY_PASSWORD, encrypt(password));
        } else {
            clearCredentials();
        }
    }

    public String[] loadCredentials() {
        if (preferences.getBoolean(KEY_REMEMBER_ME, false)) {
            String username = preferences.get(KEY_USERNAME, "");
            String password = decrypt(preferences.get(KEY_PASSWORD, ""));
            return new String[]{username, password};
        }
        return null;
    }

    public static void clearCredentials() {
        preferences.remove(KEY_REMEMBER_ME);
        preferences.remove(KEY_USERNAME);
        preferences.remove(KEY_PASSWORD);
    }

    private static String encrypt(String data) {
        return data;
    }

    private String decrypt(String data) {
        return data;
    }
}