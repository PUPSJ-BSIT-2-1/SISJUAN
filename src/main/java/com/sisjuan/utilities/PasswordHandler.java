/**
 * Utility class for handling password hashing and verification.
 * This class provides methods to hash passwords using PBKDF2 with HMAC SHA-256,
 * generate secure random salts, and verify hashed passwords.
 */

package com.sisjuan.utilities;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordHandler {

    private static final Logger logger = LoggerFactory.getLogger(PasswordHandler.class); 
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    private static final int ITERATIONS = 10000;

    public static String hashPassword(String password) {
        try {
            System.out.println("--- Hashing New Password ---"); 
            System.out.println("Input Password to Hash: '" + password + "'"); 

            byte[] salt = generateSalt();
            System.out.println("Generated Salt (Base64): '" + Base64.getEncoder().encodeToString(salt) + "'"); 

            byte[] hashedPassword = hashPasswordWithSalt(password, salt);
            String B64HashedPassword = Base64.getEncoder().encodeToString(hashedPassword);
            System.out.println("Computed Hash (Base64): '" + B64HashedPassword + "'"); 

            String fullStoredValue = Base64.getEncoder().encodeToString(salt) + ":" + B64HashedPassword;
            System.out.println("Full Value to be Stored: '" + fullStoredValue + "'"); 
            System.out.println("--- End Hashing New Password ---"); 
            return fullStoredValue;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Error while hashing password", e); 
            throw new RuntimeException("Error while hashing password", e);
        }
    }

    public static boolean verifyPassword(String password, String storedPassword) {
        try {
            System.out.println("--- Verifying Password ---"); 
            System.out.println("Input Password for Verification: '" + password + "'"); 
            System.out.println("Stored Password (DB) for Verification: '" + storedPassword + "'"); 

            String[] parts = storedPassword.split(":");
            if (parts.length != 2) {
                System.err.println("Verify Error: Invalid stored password format. Expected 'salt:hash', got: '" + storedPassword + "'"); 
                throw new IllegalArgumentException("Invalid stored password format");
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);

            System.out.println("Verify - Decoded Salt (Base64): '" + parts[0] + "'"); 
            System.out.println("Verify - Decoded Stored Hash (Base64): '" + parts[1] + "'"); 

            byte[] hashedPassword = hashPasswordWithSalt(password, salt);
            System.out.println("Verify - Newly Computed Hash (Base64 from input password + DB salt): '" + Base64.getEncoder().encodeToString(hashedPassword) + "'"); 
            
            boolean match = slowEquals(storedHash, hashedPassword);
            System.out.println("Verify - Password Match Result: " + match); 
            System.out.println("--- End Verification ---"); 
            return match;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Error while verifying password", e); 
            throw new RuntimeException("Error while verifying password", e);
        } catch (IllegalArgumentException iae) { 
            logger.error("Argument error during password verification: {}", iae.getMessage()); 
            System.err.println("Verify Error - IllegalArgumentException: " + iae.getMessage());
            throw iae; 
        }
    }

    private static byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private static byte[] hashPasswordWithSalt(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return keyFactory.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}