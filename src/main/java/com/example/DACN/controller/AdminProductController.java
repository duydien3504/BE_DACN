package com.example.DACN.controller;

import com.example.DACN.dto.request.UpdateProductStatusRequest;
import com.example.DACN.dto.response.UpdateProductStatusResponse;
import com.example.DACN.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@Tag(name = "Product Management (Admin)", description = "APIs for admins to manage products")
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "Update product status", description = "Update the status of a product (Active/Banned)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product status updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateProductStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not an admin"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{productId}/status")
    public ResponseEntity<UpdateProductStatusResponse> updateProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        return ResponseEntity.ok(productService.updateProductStatus(productId, request));
    }
}
