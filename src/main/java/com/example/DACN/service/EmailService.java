package com.example.DACN.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send password reset OTP email
     * 
     * @param toEmail Recipient email address
     * @param otpCode 6-digit OTP code
     */
    public void sendPasswordResetEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Code");
            message.setText(buildEmailContent(otpCode));

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email. Please try again later.");
        }
    }

    private String buildEmailContent(String otpCode) {
        return String.format(
                "Hello,\n\n" +
                        "You have requested to reset your password.\n\n" +
                        "Your password reset code is: %s\n\n" +
                        "This code will expire in 15 minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "DACN Team",
                otpCode);
    }
}
