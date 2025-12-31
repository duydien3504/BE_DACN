package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update order status")
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(Shipping|Delivered)$", message = "Status must be either 'Shipping' or 'Delivered'")
    @Schema(description = "New order status", example = "Shipping", allowableValues = { "Shipping", "Delivered" })
    private String status;
}
