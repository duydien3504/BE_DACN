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
@Schema(description = "Update profile response")
public class UpdateProfileResponse {

    @Schema(description = "Success message", example = "Profile updated successfully")
    String message;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId;

    @Schema(description = "Full name", example = "John Doe")
    String fullName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    String email;

    @Schema(description = "Avatar URL", example = "https://res.cloudinary.com/...")
    String avatarUrl;

    @Schema(description = "Role name", example = "Customer")
    String role;

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt;
}
