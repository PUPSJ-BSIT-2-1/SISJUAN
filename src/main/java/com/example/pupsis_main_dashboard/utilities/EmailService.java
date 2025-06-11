/**
 * EmailService class for sending verification emails.
 * This class uses JavaMail API to send emails via SMTP.
 * It is configured to use Gmail's SMTP server.
 */

package com.example.pupsis_main_dashboard.utilities;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {
    private final String username;
    private final String password;
    private final Properties props;

    public EmailService() {
        this.username = "pupsissystem@gmail.com";
        this.password = "nfbe tlex gjub hvrj";
        
        props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
    }

    public void sendVerificationEmail(String recipient, String code) throws MessagingException {
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            
            // Set proper headers
            message.setFrom(new InternetAddress(username, "PUPSIS System"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject("PUPSIS Registration: Verification Code");
            message.setHeader("X-Mailer", "JavaMail");
            message.setHeader("Precedence", "bulk");
            
            MimeMultipart multipart = getMimeMultipart(code);
            message.setContent(multipart);
            
            Transport.send(message);
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Encoding error", e);
        }
    }

    public void sendNotificationEmail(String recipient, String subject, String body) throws MessagingException {
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "PUPSIS System Notification"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);
            message.setHeader("X-Mailer", "JavaMail");
            message.setHeader("Precedence", "bulk"); // Consider if this is always appropriate

            // For simplicity, sending plain text notifications. Can be enhanced with HTML.
            MimeMultipart multipart = new MimeMultipart();

            // Text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);

            // HTML part (optional, but good for richer formatting if desired later)
            MimeBodyPart htmlPart = new MimeBodyPart();
            String htmlContent = "<html><body><p>" + body.replace("\n", "<br>") + "</p></body></html>";
            htmlPart.setContent(htmlContent, "text/html");

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart); // Choose one or both based on needs

            message.setContent(multipart);

            Transport.send(message);
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Encoding error while sending notification email", e);
        } catch (MessagingException e) {
            throw new MessagingException("Error while sending notification email", e);
        }
    }

    public void sendAcceptanceEmail(String recipient, String studentName, String sectionName) throws MessagingException {
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "PUPSIS Admissions Office"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject("Congratulations on Your Acceptance to PUP!");

            String body = "Dear " + studentName + ",\n\n"
                    + "Congratulations! We are delighted to inform you of your acceptance to the Polytechnic University of the Philippines San Juan Branch.\n\n"
                    + "You have been assigned to section: " + sectionName + ".\n\n"
                    + "Further details regarding your enrollment and orientation will be sent to you shortly. "
                    + "Please monitor your email for updates.\n\n"
                    + "Welcome to the PUP community!\n\n"
                    + "Sincerely,\n"
                    + "The PUP Admissions Office";

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);

            message.setContent(multipart);

            Transport.send(message);
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Encoding error while sending acceptance email", e);
        }
    }

    private static MimeMultipart getMimeMultipart(String code) throws MessagingException {
        MimeMultipart multipart = new MimeMultipart();

        // Text part
        MimeBodyPart textPart = new MimeBodyPart();
        String textContent = "Your PUPSIS verification code is: " + code + "\n\n"
                + "This code will expire in 15 minutes.\n"
                + "If you didn't request this, please ignore this email.";
        textPart.setText(textContent);

        // HTML part
        MimeBodyPart htmlPart = new MimeBodyPart();
        String htmlContent = "<html><body>"
                + "<h3>PUPSIS Registration</h3>"
                + "<p>Your verification code is: <strong>" + code + "</strong></p>"
                + "<p>This code will expire in 15 minutes.</p>"
                + "<p>If you didn't request this, please ignore this email.</p>"
                + "</body></html>";
        htmlPart.setContent(htmlContent, "text/html");

        // Add parts to a message
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);
        return multipart;
    }

}
