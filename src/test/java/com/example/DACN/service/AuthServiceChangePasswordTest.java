package com.example.DACN.service;

import com.example.DACN.dto.request.ChangePasswordRequest;
import com.example.DACN.dto.response.ChangePasswordResponse;
import com.example.DACN.entity.User;
import com.example.DACN.exception.AuthenticationFailedException;
import com.example.DACN.mapper.UserMapper;
import com.example.DACN.repository.PasswordResetTokenRepository;
import com.example.DACN.repository.RoleRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService ChangePassword Tests")
class AuthServiceChangePasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private ChangePasswordRequest request;
    private User user;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userEmail = "test@example.com";
        request = new ChangePasswordRequest("oldpassword123", "newpassword123");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(userEmail);
        user.setPasswordHash("hashedOldPassword");
    }

    @Test
    @DisplayName("Should change password successfully with correct old password")
    void testChangePasswordSuccess() {
        // Given
        String newHashedPassword = "hashedNewPassword";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(newHashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ChangePasswordResponse response = authService.changePassword(userEmail, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Password changed successfully");

        verify(userRepository).findByEmail(userEmail);
        verify(passwordEncoder).matches(request.getOldPassword(), user.getPasswordHash());
        verify(passwordEncoder).encode(request.getNewPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testChangePasswordUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(userEmail, request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail(userEmail);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when old password is incorrect")
    void testChangePasswordIncorrectOldPassword() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(userEmail, request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Old password is incorrect");

        verify(userRepository).findByEmail(userEmail);
        verify(passwordEncoder).matches(request.getOldPassword(), user.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should hash new password before saving")
    void testPasswordHashing() {
        // Given
        String newHashedPassword = "hashedNewPassword";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn(newHashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authService.changePassword(userEmail, request);

        // Then
        verify(passwordEncoder).encode(request.getNewPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should not change password if old password does not match")
    void testOldPasswordVerification() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword(userEmail, request))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(passwordEncoder).matches(request.getOldPassword(), user.getPasswordHash());
        verify(userRepository, never()).save(any());
    }
}
