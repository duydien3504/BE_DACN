package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order status history entry")
public class OrderStatusHistoryResponse {

    @Schema(description = "History entry ID", example = "1")
    private Long historyId;

    @Schema(description = "Order status", example = "Pending")
    private String status;

    @Schema(description = "Status description", example = "Order created and pending confirmation")
    private String description;

    @Schema(description = "Timestamp when status was set", example = "2025-12-31T09:00:00")
    private LocalDateTime createdAt;
}
