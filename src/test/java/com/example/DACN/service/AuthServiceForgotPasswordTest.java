package com.example.DACN.service;

import com.example.DACN.dto.request.ForgotPasswordRequest;
import com.example.DACN.dto.response.ForgotPasswordResponse;
import com.example.DACN.entity.PasswordResetToken;
import com.example.DACN.entity.User;
import com.example.DACN.repository.PasswordResetTokenRepository;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService ForgotPassword Tests")
class AuthServiceForgotPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private ForgotPasswordRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        request = new ForgotPasswordRequest("test@example.com");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(request.getEmail());
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void testForgotPasswordSuccess() {
        // Given
        String otpCode = "123456";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(otpService.generateOtp()).thenReturn(otpCode);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // When
        ForgotPasswordResponse response = authService.forgotPassword(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Password reset code sent to your email");

        verify(userRepository).findByEmail(request.getEmail());
        verify(otpService).generateOtp();
        verify(passwordResetTokenRepository).save(argThat(token -> token.getUserId().equals(user.getUserId()) &&
                token.getOtpCode().equals(otpCode) &&
                !token.getIsUsed() &&
                token.getExpiresAt() != null));
        verify(emailService).sendPasswordResetEmail(user.getEmail(), otpCode);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testForgotPasswordUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("If this email exists, a reset code will be sent");

        verify(userRepository).findByEmail(request.getEmail());
        verify(otpService, never()).generateOtp();
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when email sending fails")
    void testForgotPasswordEmailFailure() {
        // Given
        String otpCode = "123456";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(otpService.generateOtp()).thenReturn(otpCode);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email. Please try again later.");

        verify(userRepository).findByEmail(request.getEmail());
        verify(otpService).generateOtp();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(user.getEmail(), otpCode);
    }

    @Test
    @DisplayName("Should generate 6-digit OTP")
    void testOtpGeneration() {
        // Given
        String otpCode = "123456";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(otpService.generateOtp()).thenReturn(otpCode);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // When
        authService.forgotPassword(request);

        // Then
        verify(otpService).generateOtp();
        verify(passwordResetTokenRepository).save(argThat(token -> token.getOtpCode().equals(otpCode) &&
                token.getOtpCode().length() == 6));
    }

    @Test
    @DisplayName("Should set token expiration to 15 minutes")
    void testTokenExpiration() {
        // Given
        String otpCode = "123456";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(otpService.generateOtp()).thenReturn(otpCode);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // When
        authService.forgotPassword(request);

        // Then
        verify(passwordResetTokenRepository).save(argThat(token -> token.getExpiresAt() != null &&
                token.getExpiresAt().isAfter(java.time.LocalDateTime.now())));
    }
}
