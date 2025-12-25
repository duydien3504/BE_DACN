package com.example.DACN.service;

import com.example.DACN.dto.response.UpdateProfileResponse;
import com.example.DACN.entity.Role;
import com.example.DACN.entity.User;
import com.example.DACN.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService UpdateProfile Tests")
class UserServiceUpdateProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private MultipartFile avatar;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role role;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userEmail = "test@example.com";

        role = new Role();
        role.setRoleId(1L);
        role.setRoleName("Customer");

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Old Name");
        user.setEmail(userEmail);
        user.setAvatarUrl("https://old-avatar.com/image.jpg");
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should update full name only")
    void testUpdateFullNameOnly() throws IOException {
        // Given
        String newFullName = "New Name";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UpdateProfileResponse response = userService.updateProfile(userEmail, newFullName, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        assertThat(response.getFullName()).isEqualTo(newFullName);
        assertThat(response.getAvatarUrl()).isEqualTo(user.getAvatarUrl());

        verify(userRepository).findByEmail(userEmail);
        verify(userRepository).save(any(User.class));
        verify(cloudinaryService, never()).uploadImage(any());
        verify(cloudinaryService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("Should update avatar only")
    void testUpdateAvatarOnly() throws IOException {
        // Given
        String newAvatarUrl = "https://new-avatar.com/image.jpg";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadImage(any(MultipartFile.class))).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(avatar.isEmpty()).thenReturn(false);

        // When
        UpdateProfileResponse response = userService.updateProfile(userEmail, null, avatar);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        assertThat(response.getAvatarUrl()).isEqualTo(newAvatarUrl);

        verify(cloudinaryService).deleteImage(user.getAvatarUrl());
        verify(cloudinaryService).uploadImage(avatar);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update both full name and avatar")
    void testUpdateBothFullNameAndAvatar() throws IOException {
        // Given
        String newFullName = "New Name";
        String newAvatarUrl = "https://new-avatar.com/image.jpg";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadImage(any(MultipartFile.class))).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(avatar.isEmpty()).thenReturn(false);

        // When
        UpdateProfileResponse response = userService.updateProfile(userEmail, newFullName, avatar);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        assertThat(response.getFullName()).isEqualTo(newFullName);
        assertThat(response.getAvatarUrl()).isEqualTo(newAvatarUrl);
        assertThat(response.getRole()).isEqualTo(role.getRoleName());

        verify(cloudinaryService).deleteImage(user.getAvatarUrl());
        verify(cloudinaryService).uploadImage(avatar);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testUpdateProfileUserNotFound() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(userEmail, "New Name", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail(userEmail);
        verify(userRepository, never()).save(any());
        verify(cloudinaryService, never()).uploadImage(any());
    }

    @Test
    @DisplayName("Should not update when full name is empty")
    void testUpdateProfileEmptyFullName() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UpdateProfileResponse response = userService.updateProfile(userEmail, "   ", null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Old Name"); // Should remain unchanged

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete old avatar before uploading new one")
    void testDeleteOldAvatarBeforeUpload() throws IOException {
        // Given
        String newAvatarUrl = "https://new-avatar.com/image.jpg";
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadImage(any(MultipartFile.class))).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(avatar.isEmpty()).thenReturn(false);

        // When
        userService.updateProfile(userEmail, null, avatar);

        // Then
        verify(cloudinaryService).deleteImage(user.getAvatarUrl());
        verify(cloudinaryService).uploadImage(avatar);
    }
}
