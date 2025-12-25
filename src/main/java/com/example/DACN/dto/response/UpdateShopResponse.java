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
@Schema(description = "Response after updating shop information")
public class UpdateShopResponse {

    @Schema(description = "Shop ID", example = "1")
    private Long shopId;

    @Schema(description = "Shop name", example = "My Awesome Shop")
    private String shopName;

    @Schema(description = "Shop description", example = "We sell the best products")
    private String shopDescription;

    @Schema(description = "Logo URL", example = "https://res.cloudinary.com/...")
    private String logoUrl;

    @Schema(description = "Whether shop is approved", example = "true")
    private Boolean isApproved;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
