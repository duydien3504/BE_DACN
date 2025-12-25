package com.example.DACN.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update shop information")
public class UpdateShopRequest {

    @Schema(description = "Shop name", example = "My Awesome Shop")
    @Size(min = 3, max = 100, message = "Shop name must be between 3 and 100 characters")
    private String shopName;

    @Schema(description = "Shop description", example = "We sell the best products")
    @Size(max = 500, message = "Shop description must not exceed 500 characters")
    private String shopDescription;
}
