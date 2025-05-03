package com.example.pupsis_main_dashboard.utility;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {
    private final String username;
    private final String password;
    private final Properties props;

    public EmailService(String username, String password) {
        this.username = username;
        this.password = password;
        
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
