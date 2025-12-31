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
@Schema(description = "Seller order information")
public class SellerOrderResponse {

    @Schema(description = "Order ID", example = "123")
    private Long orderId;

    @Schema(description = "Customer email", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "Customer full name", example = "John Doe")
    private String customerName;

    @Schema(description = "Total amount before discounts", example = "1000000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Shipping fee", example = "30000.00")
    private BigDecimal shippingFee;

    @Schema(description = "Voucher discount amount", example = "50000.00")
    private BigDecimal voucherDiscount;

    @Schema(description = "Final amount after all discounts", example = "980000.00")
    private BigDecimal finalAmount;

    @Schema(description = "Payment method", example = "PAYPAL")
    private String paymentMethod;

    @Schema(description = "Current order status", example = "Pending")
    private String currentStatus;

    @Schema(description = "Order creation time", example = "2025-12-28T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Number of items in order", example = "3")
    private Integer itemCount;
}
