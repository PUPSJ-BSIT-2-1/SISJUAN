/**
 * Utility class for common validation operations.
 * This class provides methods to validate email addresses, check for numbers in strings,
 * and validate password strength.
 */

package com.example.pupsis_main_dashboard.utility;

public class ValidationUtils {

    public static boolean containsNumbers(String input) {
        return input != null && input.matches(".*\\d.*");
    }

    public static boolean isValidEmail(String email) {
        return email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
    
    public static boolean isStrongPassword(String password) {
        return password != null && 
               password.length() >= 8 && 
               password.matches(".*[a-zA-Z].*") && 
               password.matches(".*\\d.*");
    }
}