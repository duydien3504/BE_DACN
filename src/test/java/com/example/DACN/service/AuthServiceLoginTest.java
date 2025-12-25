package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.constant.UserStatus;
import com.example.DACN.dto.request.LoginRequest;
import com.example.DACN.dto.response.LoginResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.exception.AuthenticationFailedException;
import com.example.DACN.mapper.UserMapper;
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
@DisplayName("AuthService Login Tests")
class AuthServiceLoginTest {

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

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private User user;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("test@example.com", "password123");

        customerRole = new Role();
        customerRole.setRoleId(1L);
        customerRole.setRoleName(RoleConstants.CUSTOMER);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(loginRequest.getEmail());
        user.setPasswordHash("hashedPassword");
        user.setRole(customerRole);
        user.setStatus(UserStatus.ACTIVE);
        user.setHasDeleted(false);
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() {
        // Given
        String expectedToken = "jwt.token.here";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn(expectedToken);

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(expectedToken);
        assertThat(response.getRole()).isEqualTo(RoleConstants.CUSTOMER);

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPasswordHash());
        verify(jwtUtil).generateToken(eq(user.getEmail()), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testLoginUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid")
    void testLoginInvalidPassword() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when user is banned")
    void testLoginUserBanned() {
        // Given
        user.setStatus(UserStatus.BANNED);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Account has been banned");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should throw exception when user is deleted")
    void testLoginUserDeleted() {
        // Given
        user.setHasDeleted(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Account not found");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPasswordHash());
        verify(jwtUtil, never()).generateToken(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should include user claims in JWT token")
    void testLoginTokenContainsClaims() {
        // Given
        String expectedToken = "jwt.token.here";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn(expectedToken);

        // When
        authService.login(loginRequest);

        // Then
        verify(jwtUtil).generateToken(eq(user.getEmail()), argThat(claims -> claims.containsKey("userId") &&
                claims.containsKey("roleId") &&
                claims.containsKey("role") &&
                claims.get("role").equals(RoleConstants.CUSTOMER)));
    }
}
