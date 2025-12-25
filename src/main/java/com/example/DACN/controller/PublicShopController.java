package com.example.DACN.controller;

import com.example.DACN.dto.response.ProductListResponse;
import com.example.DACN.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Validated
@Tag(name = "Shop Management (Public)", description = "Public APIs for browsing shops and their products")
public class PublicShopController {

    private final ProductService productService;

    @GetMapping("/{shopId}/products")
    @Operation(summary = "Get shop products", description = "Get a paginated list of products from a specific shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<ProductListResponse> getShopProducts(
            @PathVariable("shopId") Long shopId,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(value = 0, message = "Page must be non-negative") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") int size) {

        ProductListResponse response = productService.getProductsByShopId(shopId, page, size);
        return ResponseEntity.ok(response);
    }
}
