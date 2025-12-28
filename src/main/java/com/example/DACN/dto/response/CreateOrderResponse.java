package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Response after successfully creating an order")
public class CreateOrderResponse {

    @Schema(description = "Created order ID", example = "100")
    Long orderId;

    @Schema(description = "Payment URL for PayPal (null if COD)", example = "https://www.sandbox.paypal.com/checkoutnow?token=EC-xxx", nullable = true)
    String paymentUrl;
}
