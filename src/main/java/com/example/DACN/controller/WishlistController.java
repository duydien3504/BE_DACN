package com.example.DACN.controller;

import com.example.DACN.dto.request.WishlistRequest;
import com.example.DACN.dto.response.WishlistResponse;
import com.example.DACN.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.example.DACN.dto.response.DeleteWishlistResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist management APIs")
public class WishlistController {

        private final WishlistService wishlistService;

        @Operation(summary = "Add product to wishlist", description = "Add a product to the authenticated user's wishlist")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Added to wishlist successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishlistResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "404", description = "Product or User not found"),
                        @ApiResponse(responseCode = "409", description = "Product already in wishlist")
        })
        @PreAuthorize("hasRole('CUSTOMER')")
        @PostMapping
        public ResponseEntity<WishlistResponse> createWishlist(
                        @Valid @RequestBody WishlistRequest request,
                        Authentication authentication) {
                String email = authentication.getName();
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(wishlistService.createWishlist(request, email));
        }

        @Operation(summary = "Remove product from wishlist", description = "Remove a product from the authenticated user's wishlist")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Removed from wishlist successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeleteWishlistResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "404", description = "Wishlist item not found")
        })
        @PreAuthorize("hasRole('CUSTOMER')")
        @DeleteMapping("/{wishlistId}")
        public ResponseEntity<DeleteWishlistResponse> deleteWishlist(
                        @PathVariable Long wishlistId,
                        Authentication authentication) {
                String email = authentication.getName();
                return ResponseEntity.ok(wishlistService.deleteWishlist(wishlistId, email));
        }

        @Operation(summary = "Get user wishlist", description = "Retrieve the authenticated user's wishlist")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Retrieved wishlist successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishlistResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @PreAuthorize("hasRole('CUSTOMER')")
        @GetMapping
        public ResponseEntity<List<WishlistResponse>> getWishlist(Authentication authentication) {
                String email = authentication.getName();
                return ResponseEntity.ok(wishlistService.getWishlist(email));
        }
}
