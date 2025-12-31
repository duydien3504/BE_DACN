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
@Schema(description = "Response after updating order status")
public class UpdateOrderStatusResponse {

    @Schema(description = "Order ID", example = "123")
    private Long orderId;

    @Schema(description = "Updated status", example = "Shipping")
    private String status;

    @Schema(description = "Status description", example = "Order is being shipped")
    private String description;

    @Schema(description = "Timestamp of status update", example = "2025-12-31T08:54:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Success message", example = "Order status updated successfully")
    private String message;
}
