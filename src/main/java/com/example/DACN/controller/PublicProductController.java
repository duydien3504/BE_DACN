package com.example.DACN.controller;

import com.example.DACN.dto.response.ProductDetailResponse;
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

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Product Management (Public)", description = "Public APIs for browsing products")
public class PublicProductController {

        private final ProductService productService;

        @GetMapping
        @Operation(summary = "Get products list", description = "Get a paginated list of products with optional filters and sorting")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
        })
        public ResponseEntity<ProductListResponse> getProducts(
                        @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                        @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                        @RequestParam(value = "category_id", required = false) Long categoryId,
                        @RequestParam(value = "shop_id", required = false) Long shopId,
                        @RequestParam(value = "sort_by", required = false, defaultValue = "created_at") String sortBy,
                        @RequestParam(value = "page", required = false, defaultValue = "0") @Min(value = 0, message = "Page must be non-negative") int page,
                        @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1, message = "Size must be at least 1") int size) {

                ProductListResponse response = productService.getProducts(
                                minPrice, maxPrice, categoryId, shopId, sortBy, page, size);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{productId}")
        @Operation(summary = "Get product details", description = "Get detailed information about a specific product including images and shop details")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Product not found")
        })
        public ResponseEntity<ProductDetailResponse> getProductById(
                        @PathVariable("productId") Long productId) {

                ProductDetailResponse response = productService.getProductById(productId);
                return ResponseEntity.ok(response);
        }
}
