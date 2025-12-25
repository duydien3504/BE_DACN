package com.example.DACN.service;

import com.example.DACN.dto.response.UpdateProfileResponse;
import com.example.DACN.dto.response.UserProfileResponse;
import com.example.DACN.entity.User;
import com.example.DACN.mapper.UserMapper;
import com.example.DACN.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        log.info("Getting profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        log.info("Profile retrieved successfully for user: {}", email);
        return userMapper.toUserProfileResponse(user);
    }

    @Transactional
    public UpdateProfileResponse updateProfile(String email, String fullName, MultipartFile avatar) throws IOException {
        log.info("Updating profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new RuntimeException("User not found");
                });

        // Update full name
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
            log.info("Updated full name for user: {}", email);
        }

        // Upload avatar if provided
        if (avatar != null && !avatar.isEmpty()) {
            // Delete old avatar if exists
            if (user.getAvatarUrl() != null) {
                cloudinaryService.deleteImage(user.getAvatarUrl());
            }

            // Upload new avatar
            String avatarUrl = cloudinaryService.uploadImage(avatar);
            user.setAvatarUrl(avatarUrl);
            log.info("Updated avatar for user: {}", email);
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", email);

        return UpdateProfileResponse.builder()
                .message("Profile updated successfully")
                .userId(savedUser.getUserId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .avatarUrl(savedUser.getAvatarUrl())
                .role(savedUser.getRole().getRoleName())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }
}
