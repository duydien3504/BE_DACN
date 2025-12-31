package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new voucher")
public class CreateVoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(min = 3, max = 50, message = "Voucher code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Voucher code must contain only uppercase letters and numbers")
    @Schema(description = "Unique voucher code", example = "GIAM20")
    private String code;

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "^(PERCENT|FIXED)$", message = "Discount type must be either PERCENT or FIXED")
    @Schema(description = "Type of discount", example = "PERCENT", allowableValues = { "PERCENT", "FIXED" })
    private String discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Schema(description = "Discount value (percentage or fixed amount)", example = "20")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Minimum order value must be greater than or equal to 0")
    @Schema(description = "Minimum order value to apply voucher", example = "100000")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0", message = "Maximum discount amount must be greater than or equal to 0")
    @Schema(description = "Maximum discount amount (for PERCENT type)", example = "50000")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    @Schema(description = "Voucher start date", example = "2025-01-01T00:00:00")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Schema(description = "Voucher end date", example = "2025-12-31T23:59:59")
    private LocalDateTime endDate;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Number of vouchers available", example = "100")
    private Integer quantity;
}
