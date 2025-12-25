package com.example.DACN.service;

import com.example.DACN.dto.response.UserProfileResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.mapper.UserMapper;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService GetProfile Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role role;
    private UserProfileResponse profileResponse;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userEmail = "test@example.com";

        role = new Role();
        role.setRoleId(1L);
        role.setRoleName("Customer");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Test User");
        user.setEmail(userEmail);
        user.setPhoneNumber("+84123456789");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setRole(role);
        user.setStatus("Active");
        user.setIsEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        profileResponse = UserProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(role.getRoleName())
                .status(user.getStatus())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Should get user profile successfully")
    void testGetProfileSuccess() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMapper.toUserProfileResponse(any(User.class))).thenReturn(profileResponse);

        // When
        UserProfileResponse result = userService.getProfile(userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(user.getUserId());
        assertThat(result.getFullName()).isEqualTo(user.getFullName());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getPhoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(result.getRole()).isEqualTo(role.getRoleName());
        assertThat(result.getStatus()).isEqualTo(user.getStatus());
        assertThat(result.getIsEmailVerified()).isEqualTo(user.getIsEmailVerified());

        verify(userRepository).findByEmail(userEmail);
        verify(userMapper).toUserProfileResponse(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testGetProfileUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getProfile(userEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail(userEmail);
        verify(userMapper, never()).toUserProfileResponse(any());
    }

    @Test
    @DisplayName("Should return profile with role information")
    void testGetProfileWithRole() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMapper.toUserProfileResponse(any(User.class))).thenReturn(profileResponse);

        // When
        UserProfileResponse result = userService.getProfile(userEmail);

        // Then
        assertThat(result.getRole()).isNotNull();
        assertThat(result.getRole()).isEqualTo("Customer");

        verify(userRepository).findByEmail(userEmail);
        verify(userMapper).toUserProfileResponse(user);
    }

    @Test
    @DisplayName("Should use mapper to convert entity to response")
    void testMapperUsage() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMapper.toUserProfileResponse(any(User.class))).thenReturn(profileResponse);

        // When
        userService.getProfile(userEmail);

        // Then
        verify(userMapper).toUserProfileResponse(user);
    }
}
