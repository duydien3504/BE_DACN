package com.example.DACN.service;

import com.example.DACN.dto.request.ResetPasswordRequest;
import com.example.DACN.dto.response.ResetPasswordResponse;
import com.example.DACN.entity.PasswordResetToken;
import com.example.DACN.entity.User;
import com.example.DACN.exception.InvalidOtpException;
import com.example.DACN.repository.PasswordResetTokenRepository;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService ResetPassword Tests")
class AuthServiceResetPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private ResetPasswordRequest request;
    private User user;
    private PasswordResetToken resetToken;

    @BeforeEach
    void setUp() {
        request = new ResetPasswordRequest("test@example.com", "123456", "newpassword123");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(request.getEmail());
        user.setPasswordHash("oldHashedPassword");

        resetToken = new PasswordResetToken();
        resetToken.setTokenId(1L);
        resetToken.setUserId(user.getUserId());
        resetToken.setOtpCode(request.getOtp());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetToken.setIsUsed(false);
    }

    @Test
    @DisplayName("Should reset password successfully with valid OTP")
    void testResetPasswordSuccess() {
        // Given
        String newHashedPassword = "newHashedPassword";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(anyString())).thenReturn(newHashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        // When
        ResetPasswordResponse response = authService.resetPassword(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Password reset successfully");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordResetTokenRepository).findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                eq(user.getUserId()), eq(request.getOtp()), any(LocalDateTime.class));
        verify(passwordEncoder).encode(request.getNewPassword());
        verify(userRepository).save(argThat(savedUser -> savedUser.getPasswordHash().equals(newHashedPassword)));
        verify(passwordResetTokenRepository).save(argThat(token -> token.getIsUsed()));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testResetPasswordUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("Invalid OTP or email");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordResetTokenRepository, never()).findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                any(), anyString(), any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when OTP is invalid")
    void testResetPasswordInvalidOtp() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("Invalid or expired OTP");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordResetTokenRepository).findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                eq(user.getUserId()), eq(request.getOtp()), any(LocalDateTime.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when OTP is expired")
    void testResetPasswordExpiredOtp() {
        // Given
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty()); // Expired token won't be found

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("Invalid or expired OTP");

        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should mark token as used after successful reset")
    void testTokenMarkedAsUsed() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        // When
        authService.resetPassword(request);

        // Then
        verify(passwordResetTokenRepository).save(argThat(token -> token.getIsUsed() == true));
    }

    @Test
    @DisplayName("Should hash new password before saving")
    void testPasswordHashing() {
        // Given
        String newHashedPassword = "hashedNewPassword";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(newHashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        // When
        authService.resetPassword(request);

        // Then
        verify(passwordEncoder).encode(request.getNewPassword());
        verify(userRepository).save(argThat(savedUser -> savedUser.getPasswordHash().equals(newHashedPassword) &&
                !savedUser.getPasswordHash().equals("oldHashedPassword")));
    }
}
