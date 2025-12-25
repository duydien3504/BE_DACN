package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.constant.UserStatus;
import com.example.DACN.dto.request.RegisterRequest;
import com.example.DACN.dto.response.RegisterResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.mapper.UserMapper;
import com.example.DACN.repository.RoleRepository;
import com.example.DACN.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private User user;
    private Role customerRole;
    private RegisterResponse expectedResponse;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "John Doe",
                "0123456789");

        customerRole = new Role();
        customerRole.setRoleId(1L);
        customerRole.setRoleName(RoleConstants.CUSTOMER);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail(validRequest.getEmail());
        user.setFullName(validRequest.getFullName());
        user.setPhoneNumber(validRequest.getPhoneNumber());
        user.setRole(customerRole);
        user.setStatus(UserStatus.ACTIVE);

        expectedResponse = new RegisterResponse(user.getUserId(), "Success");
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterSuccess() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(RoleConstants.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toRegisterResponse(any(User.class))).thenReturn(expectedResponse);

        // When
        RegisterResponse response = authService.register(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(user.getUserId());
        assertThat(response.getMessage()).isEqualTo("Success");

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository).findByPhoneNumber(validRequest.getPhoneNumber());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegisterWithDuplicateEmail() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when phone number already exists")
    void testRegisterWithDuplicatePhoneNumber() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Phone number already exists");

        verify(userRepository).findByPhoneNumber(validRequest.getPhoneNumber());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when customer role not found")
    void testRegisterWhenRoleNotFound() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(RoleConstants.CUSTOMER)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer role not found");

        verify(roleRepository).findByRoleName(RoleConstants.CUSTOMER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should hash password before saving")
    void testPasswordIsHashed() {
        // Given
        String hashedPassword = "hashedPassword123";
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(RoleConstants.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toRegisterResponse(any(User.class))).thenReturn(expectedResponse);

        // When
        authService.register(validRequest);

        // Then
        verify(passwordEncoder).encode(validRequest.getPassword());
    }

    @Test
    @DisplayName("Should set user status to Active")
    void testUserStatusIsActive() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(RoleConstants.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toRegisterResponse(any(User.class))).thenReturn(expectedResponse);

        // When
        authService.register(validRequest);

        // Then
        verify(userRepository).save(argThat(savedUser -> UserStatus.ACTIVE.equals(savedUser.getStatus())));
    }
}
