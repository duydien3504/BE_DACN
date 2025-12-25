package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "User profile response")
public class UserProfileResponse {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId;

    @Schema(description = "Full name", example = "John Doe")
    String fullName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    String email;

    @Schema(description = "Phone number", example = "+84123456789")
    String phoneNumber;

    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    String avatarUrl;

    @Schema(description = "Role name", example = "Customer")
    String role;

    @Schema(description = "Account status", example = "Active")
    String status;

    @Schema(description = "Email verification status", example = "true")
    Boolean isEmailVerified;

    @Schema(description = "Account creation timestamp")
    LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt;
}
