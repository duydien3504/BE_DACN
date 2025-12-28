package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to create a new order from cart items")
public class CreateOrderRequest {

    @NotNull(message = "Shop ID is required")
    @Positive(message = "Shop ID must be positive")
    @Schema(description = "ID of the shop", example = "1")
    Long shopId;

    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    @Schema(description = "List of items to order")
    List<OrderItemRequest> items;

    @Schema(description = "Voucher ID to apply discount", example = "10")
    Long voucherId;

    @NotNull(message = "Address ID is required")
    @Positive(message = "Address ID must be positive")
    @Schema(description = "Delivery address ID", example = "5")
    Long addressId;

    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "COD|PAYPAL", message = "Payment method must be either COD or PAYPAL")
    @Schema(description = "Payment method", example = "COD", allowableValues = { "COD", "PAYPAL" })
    String paymentMethod;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Order item details")
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        @Schema(description = "Product ID", example = "1")
        Long productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @Schema(description = "Quantity to order", example = "2")
        Integer qty;
    }
}
