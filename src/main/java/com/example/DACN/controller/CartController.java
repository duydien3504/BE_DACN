package com.example.DACN.controller;

import com.example.DACN.dto.request.AddCartItemRequest;
import com.example.DACN.dto.response.AddCartItemResponse;
import com.example.DACN.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "APIs for managing shopping cart")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add item to cart", description = "Add a product to the customer's cart. If the product already exists, quantity is incremented.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    public ResponseEntity<AddCartItemResponse> addCartItem(@Valid @RequestBody AddCartItemRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        AddCartItemResponse response = cartService.addCartItem(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @org.springframework.web.bind.annotation.GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get cart", description = "Retrieve the current user's shopping cart details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    public ResponseEntity<com.example.DACN.dto.response.CartResponse> getCart() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.example.DACN.dto.response.CartResponse response = cartService.getCart(userEmail);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/items/{cartItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update cart item quantity", description = "Update the quantity of a specific item in the customer's cart.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Cart item not found"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    public ResponseEntity<com.example.DACN.dto.response.CartItemResponse> updateCartItem(
            @org.springframework.web.bind.annotation.PathVariable Long cartItemId,
            @Valid @RequestBody com.example.DACN.dto.request.UpdateCartItemRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.example.DACN.dto.response.CartItemResponse response = cartService.updateCartItem(userEmail, cartItemId,
                request);
        return ResponseEntity.ok(response);
    }
}
