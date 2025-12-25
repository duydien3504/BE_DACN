package com.example.DACN.controller;

import com.example.DACN.dto.request.WishlistRequest;
import com.example.DACN.dto.response.WishlistResponse;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.service.WishlistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import com.example.DACN.dto.response.DeleteWishlistResponse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("WishlistController Tests")
class WishlistControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private WishlistService wishlistService;

        private WishlistRequest validRequest;
        private WishlistResponse successResponse;

        @BeforeEach
        void setUp() {
                validRequest = WishlistRequest.builder()
                                .productId(1L)
                                .build();

                successResponse = WishlistResponse.builder()
                                .wishlistId(10L)
                                .productId(1L)
                                .productName("Test Product")
                                .productPrice(BigDecimal.valueOf(100.0))
                                .createdAt(LocalDateTime.now())
                                .message("Added to wishlist successfully")
                                .build();
        }

        @Test
        @DisplayName("Should add product to wishlist successfully (201 Created)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void createWishlist_Success() throws Exception {
                // Given
                when(wishlistService.createWishlist(any(WishlistRequest.class), eq("customer@example.com")))
                                .thenReturn(successResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/wishlists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.wishlistId").value(10))
                                .andExpect(jsonPath("$.productId").value(1))
                                .andExpect(jsonPath("$.message").value("Added to wishlist successfully"));

                verify(wishlistService).createWishlist(any(WishlistRequest.class), eq("customer@example.com"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when validation fails (missing productId)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void createWishlist_ValidationError() throws Exception {
                // Given
                WishlistRequest invalidRequest = new WishlistRequest(); // ProductId is null

                // When & Then
                mockMvc.perform(post("/api/v1/wishlists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 Not Found when product does not exist")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void createWishlist_ProductNotFound() throws Exception {
                // Given
                when(wishlistService.createWishlist(any(WishlistRequest.class), eq("customer@example.com")))
                                .thenThrow(new ResourceNotFoundException("Product not found"));

                // When & Then
                mockMvc.perform(post("/api/v1/wishlists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Product not found"));
        }

        @Test
        @DisplayName("Should return 409 Conflict when product is already in wishlist")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void createWishlist_Duplicate() throws Exception {
                // Given
                when(wishlistService.createWishlist(any(WishlistRequest.class), eq("customer@example.com")))
                                .thenThrow(new DuplicateResourceException("Product is already in wishlist"));

                // When & Then
                mockMvc.perform(post("/api/v1/wishlists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Product is already in wishlist"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user is not authenticated")
        void createWishlist_Unauthenticated() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/v1/wishlists")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should delete wishlist item successfully (200 OK)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void deleteWishlist_Success() throws Exception {
                // Given
                DeleteWishlistResponse deleteResponse = DeleteWishlistResponse.builder()
                                .wishlistId(10L)
                                .message("Removed from wishlist successfully")
                                .build();

                when(wishlistService.deleteWishlist(eq(10L), eq("customer@example.com")))
                                .thenReturn(deleteResponse);

                // When & Then
                mockMvc.perform(delete("/api/v1/wishlists/{wishlistId}", 10L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.wishlistId").value(10))
                                .andExpect(jsonPath("$.message").value("Removed from wishlist successfully"));

                verify(wishlistService).deleteWishlist(eq(10L), eq("customer@example.com"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when wishlist item does not exist")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void deleteWishlist_NotFound() throws Exception {
                // Given
                when(wishlistService.deleteWishlist(eq(999L), eq("customer@example.com")))
                                .thenThrow(new ResourceNotFoundException("Wishlist item not found"));

                // When & Then
                mockMvc.perform(delete("/api/v1/wishlists/{wishlistId}", 999L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Wishlist item not found"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when delete user is not a CUSTOMER")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void deleteWishlist_WrongRole() throws Exception {
                // When & Then
                mockMvc.perform(delete("/api/v1/wishlists/{wishlistId}", 10L))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return user wishlist successfully (200 OK)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void getWishlist_Success() throws Exception {
                // Given
                when(wishlistService.getWishlist(eq("customer@example.com")))
                                .thenReturn(java.util.Collections.singletonList(successResponse));

                // When & Then
                mockMvc.perform(get("/api/v1/wishlists"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].wishlistId").value(10))
                                .andExpect(jsonPath("$[0].productId").value(1))
                                .andExpect(jsonPath("$[0].message").value("Added to wishlist successfully"));

                verify(wishlistService).getWishlist(eq("customer@example.com"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when get wishlist user is not a CUSTOMER")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void getWishlist_WrongRole() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/v1/wishlists"))
                                .andExpect(status().isForbidden());
        }
}
