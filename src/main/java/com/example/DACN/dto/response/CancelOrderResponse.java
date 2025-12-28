package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for order cancellation")
public class CancelOrderResponse {

    @Schema(description = "Order ID", example = "123")
    private Long orderId;

    @Schema(description = "Cancellation message", example = "Order cancelled successfully")
    private String message;

    @Schema(description = "Cancelled status", example = "true")
    private Boolean cancelled;
}
