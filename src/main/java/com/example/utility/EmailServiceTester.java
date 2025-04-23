package com.example.utility;

import java.util.Scanner;

public class EmailServiceTester {
    // Configure these constants with your test credentials
    private static final String TEST_GMAIL = "harolddelapena.11@gmail.com";
    private static final String TEST_PASSWORD = "sfhq xeks hgeo yfja";
    private static final String DEFAULT_RECIPIENT = "gratedestroyer99@gmail.com";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Email Verification Tester ===");
        System.out.print("Enter recipient email (press enter to use default " + DEFAULT_RECIPIENT + "): ");
        String recipient = scanner.nextLine();
        
        if (recipient.isEmpty()) {
            recipient = DEFAULT_RECIPIENT;
        }
        
        EmailService emailService = new EmailService(TEST_GMAIL, TEST_PASSWORD);
        
        try {
            String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));
            System.out.println("\nSending verification code " + verificationCode + " to " + recipient);
            
            emailService.sendVerificationEmail(recipient, verificationCode);
            System.out.println("Email sent successfully!");
            
            System.out.print("\nEnter the verification code you received: ");
            String userInput = scanner.nextLine();
            
            if(userInput.equals(verificationCode)) {
                System.out.println("Verification successful!");
            } else {
                System.out.println("Verification failed - codes don't match");
            }
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
