package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of error", example = "2024-01-01T10:00:00")
    LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    int status;

    @Schema(description = "Error message", example = "Validation failed")
    String message;

    @Schema(description = "List of detailed errors")
    List<String> errors;

    public ErrorResponse(LocalDateTime timestamp, int status, String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
    }
}
