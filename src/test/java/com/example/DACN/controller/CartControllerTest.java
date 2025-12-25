package com.example.DACN.controller;

import com.example.DACN.dto.request.AddCartItemRequest;
import com.example.DACN.dto.response.AddCartItemResponse;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.service.CartService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartController Tests")
class CartControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CartService cartService;

        private AddCartItemRequest validRequest;
        private AddCartItemResponse successResponse;

        @BeforeEach
        void setUp() {
                validRequest = AddCartItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build();

                successResponse = AddCartItemResponse.builder()
                                .cartItemId(1L)
                                .productId(1L)
                                .productName("Test Product")
                                .price(new BigDecimal("100.00"))
                                .quantity(2)
                                .message("Item added to cart successfully")
                                .build();
        }

        @Test
        @DisplayName("Should add item to cart successfully (201 Created)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void addCartItem_Success() throws Exception {
                // Given
                when(cartService.addCartItem(eq("customer@example.com"), any(AddCartItemRequest.class)))
                                .thenReturn(successResponse);

                // When & Then
                mockMvc.perform(post("/api/v1/carts/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.cartItemId").value(1))
                                .andExpect(jsonPath("$.productId").value(1))
                                .andExpect(jsonPath("$.quantity").value(2))
                                .andExpect(jsonPath("$.message").value("Item added to cart successfully"));

                verify(cartService).addCartItem(eq("customer@example.com"), any(AddCartItemRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when validation fails (invalid quantity)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void addCartItem_ValidationError() throws Exception {
                // Given
                AddCartItemRequest invalidRequest = AddCartItemRequest.builder()
                                .productId(1L)
                                .quantity(0) // Invalid: Must be >= 1
                                .build();

                // When & Then
                mockMvc.perform(post("/api/v1/carts/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when validation fails (null product ID)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void addCartItem_NullProductId() throws Exception {
                // Given
                AddCartItemRequest invalidRequest = AddCartItemRequest.builder()
                                .productId(null)
                                .quantity(1)
                                .build();

                // When & Then
                mockMvc.perform(post("/api/v1/carts/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 Not Found when product does not exist")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void addCartItem_ProductNotFound() throws Exception {
                // Given
                when(cartService.addCartItem(eq("customer@example.com"), any(AddCartItemRequest.class)))
                                .thenThrow(new ResourceNotFoundException("Product not found"));

                // When & Then
                mockMvc.perform(post("/api/v1/carts/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Product not found"));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user is not a CUSTOMER")
        @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
        void addCartItem_WrongRole() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/v1/carts/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user is not authenticated")
        void addCartItem_Unauthenticated() throws Exception {
                // When & Then
                mockMvc.perform(post("/api/v1/carts/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should get cart successfully (200 OK)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void getCart_Success() throws Exception {
                // Given
                com.example.DACN.dto.response.CartResponse cartResponse = com.example.DACN.dto.response.CartResponse
                                .builder()
                                .cartId(1L)
                                .totalPrice(new BigDecimal("200.00"))
                                .items(java.util.Collections.emptyList())
                                .build();

                when(cartService.getCart("customer@example.com")).thenReturn(cartResponse);

                // When & Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/carts")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cartId").value(1))
                                .andExpect(jsonPath("$.totalPrice").value(200.00));

                verify(cartService).getCart("customer@example.com");
        }

        @Test
        @DisplayName("Should update cart item successfully (200 OK)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void updateCartItem_Success() throws Exception {
                // Given
                com.example.DACN.dto.request.UpdateCartItemRequest updateRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                                .builder()
                                .quantity(5)
                                .build();

                com.example.DACN.dto.response.CartItemResponse updateResponse = com.example.DACN.dto.response.CartItemResponse
                                .builder()
                                .cartItemId(1L)
                                .productId(1L)
                                .productName("Test Product")
                                .quantity(5)
                                .build();

                when(cartService.updateCartItem(eq("customer@example.com"), eq(1L),
                                any(com.example.DACN.dto.request.UpdateCartItemRequest.class)))
                                .thenReturn(updateResponse);

                // When & Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/v1/carts/items/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.cartItemId").value(1))
                                .andExpect(jsonPath("$.quantity").value(5));

                verify(cartService).updateCartItem(eq("customer@example.com"), eq(1L),
                                any(com.example.DACN.dto.request.UpdateCartItemRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when update validation fails (invalid quantity)")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void updateCartItem_ValidationError() throws Exception {
                // Given
                com.example.DACN.dto.request.UpdateCartItemRequest invalidRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                                .builder()
                                .quantity(0) // Invalid
                                .build();

                // When & Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/v1/carts/items/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 Not Found when cart item to update does not exist")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void updateCartItem_NotFound() throws Exception {
                // Given
                com.example.DACN.dto.request.UpdateCartItemRequest updateRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                                .builder()
                                .quantity(5)
                                .build();

                when(cartService.updateCartItem(eq("customer@example.com"), eq(1L),
                                any(com.example.DACN.dto.request.UpdateCartItemRequest.class)))
                                .thenThrow(new ResourceNotFoundException("Cart item not found"));

                // When & Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/v1/carts/items/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Cart item not found"));
        }
}
