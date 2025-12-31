package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response containing voucher details")
public class VoucherResponse {

    @Schema(description = "Voucher ID", example = "1")
    private Long voucherId;

    @Schema(description = "Shop ID", example = "1")
    private Long shopId;

    @Schema(description = "Voucher code", example = "GIAM20")
    private String code;

    @Schema(description = "Discount type", example = "PERCENT")
    private String discountType;

    @Schema(description = "Discount value", example = "20")
    private BigDecimal discountValue;

    @Schema(description = "Minimum order value", example = "100000")
    private BigDecimal minOrderValue;

    @Schema(description = "Maximum discount amount", example = "50000")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Start date", example = "2025-01-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "End date", example = "2025-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "Quantity available", example = "100")
    private Integer quantity;
}
