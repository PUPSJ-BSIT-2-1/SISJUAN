package com.example.utility;

import java.util.prefs.Preferences;

public class RememberMeHandler {
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private final Preferences preferences = Preferences.userNodeForPackage(RememberMeHandler.class);

    public void saveCredentials(String username, String password, boolean rememberMe) {
        if (rememberMe) {
            preferences.putBoolean(KEY_REMEMBER_ME, true);
            preferences.put(KEY_USERNAME, username);
            preferences.put(KEY_PASSWORD, encrypt(password)); // Optionally encrypt
        } else {
            clearCredentials();
        }
    }

    public String[] loadCredentials() {
        if (preferences.getBoolean(KEY_REMEMBER_ME, false)) {
            String username = preferences.get(KEY_USERNAME, "");
            String password = decrypt(preferences.get(KEY_PASSWORD, "")); // Decrypt when retrieved
            return new String[]{username, password};
        }
        return null;
    }

    public void clearCredentials() {
        preferences.remove(KEY_REMEMBER_ME);
        preferences.remove(KEY_USERNAME);
        preferences.remove(KEY_PASSWORD);
    }

    private String encrypt(String data) {
        // Implement encryption logic here (e.g., AES)
        return data;
    }

    private String decrypt(String data) {
        // Implement decryption logic here (e.g., AES)
        return data;
    }
}