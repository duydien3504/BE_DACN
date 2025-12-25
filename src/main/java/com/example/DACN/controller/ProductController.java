package com.example.DACN.controller;

import com.example.DACN.dto.request.CreateProductRequest;
import com.example.DACN.dto.request.UpdateProductRequest;
import com.example.DACN.dto.response.CreateProductResponse;
import com.example.DACN.dto.response.DeleteProductResponse;
import com.example.DACN.dto.response.UpdateProductResponse;
import com.example.DACN.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Product Management (Seller)", description = "APIs for sellers to manage their products")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new product", description = "Allows a seller to create a new product with images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not a seller or shop not approved"),
            @ApiResponse(responseCode = "404", description = "Category not found or seller has no shop")
    })
    public ResponseEntity<CreateProductResponse> createProduct(
            Authentication authentication,
            @RequestParam("name") @NotBlank(message = "Product name is required") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,
            @RequestParam("categoryId") @NotNull(message = "Category ID is required") Long categoryId,
            @RequestParam(value = "stockQuantity", required = false, defaultValue = "0") @Min(value = 0, message = "Stock quantity must be non-negative") Integer stockQuantity,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) throws IOException {

        String email = authentication.getName();

        // Create request object
        CreateProductRequest request = new CreateProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setCategoryId(categoryId);
        request.setStockQuantity(stockQuantity);

        CreateProductResponse response = productService.createProduct(email, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a product", description = "Allows a seller to update their product with optional new images")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not a seller or product doesn't belong to seller"),
            @ApiResponse(responseCode = "404", description = "Product not found or category not found")
    })
    public ResponseEntity<UpdateProductResponse> updateProduct(
            Authentication authentication,
            @PathVariable("productId") Long productId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "stockQuantity", required = false) @Min(value = 0, message = "Stock quantity must be non-negative") Integer stockQuantity,
            @RequestParam(value = "status", required = false) @Pattern(regexp = "^(Active|Inactive)$", message = "Status must be either 'Active' or 'Inactive'") String status,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) throws IOException {

        String email = authentication.getName();

        // Create request object
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setCategoryId(categoryId);
        request.setStockQuantity(stockQuantity);
        request.setStatus(status);

        UpdateProductResponse response = productService.updateProduct(email, productId, request, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete a product", description = "Allows a seller to soft delete their product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user is not a seller or product doesn't belong to seller"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<DeleteProductResponse> deleteProduct(
            Authentication authentication,
            @PathVariable("productId") Long productId) {

        String email = authentication.getName();

        DeleteProductResponse response = productService.deleteProduct(email, productId);
        return ResponseEntity.ok(response);
    }
}
