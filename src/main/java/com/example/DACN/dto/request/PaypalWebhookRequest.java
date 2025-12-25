package com.example.DACN.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "PayPal webhook event request")
public class PaypalWebhookRequest {

    @JsonProperty("event_type")
    @Schema(description = "Type of PayPal event", example = "PAYMENT.CAPTURE.COMPLETED")
    String eventType;

    @JsonProperty("resource")
    @Schema(description = "Resource data from PayPal")
    Resource resource;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Resource {

        @JsonProperty("supplementary_data")
        SupplementaryData supplementaryData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SupplementaryData {

        @JsonProperty("related_ids")
        RelatedIds relatedIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RelatedIds {

        @JsonProperty("order_id")
        @Schema(description = "PayPal order ID", example = "5O190127TN364715T")
        String orderId;
    }
}
