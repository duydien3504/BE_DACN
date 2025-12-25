package com.example.DACN.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Shop registration response")
public class ShopRegisterResponse {

    @Schema(description = "ID of the created shop", example = "1")
    Long shopId;

    @Schema(description = "PayPal order ID", example = "5O190127TN364715T")
    String paypalOrderId;

    @Schema(description = "PayPal payment approval URL", example = "https://www.sandbox.paypal.com/checkoutnow?token=5O190127TN364715T")
    String payUrl;

    @Schema(description = "Response message", example = "Shop registration initiated. Please complete payment.")
    String message;
}
