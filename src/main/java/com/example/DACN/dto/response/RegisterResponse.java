package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "User registration response")
public class RegisterResponse {

    @Schema(description = "Created user ID", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID userId;

    @Schema(description = "Success message", example = "Success")
    String message;
}
