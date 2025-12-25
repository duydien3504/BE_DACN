package com.example.DACN.controller;

import com.example.DACN.dto.request.UpdateProductStatusRequest;
import com.example.DACN.dto.response.UpdateProductStatusResponse;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.service.ProductService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AdminProductController Tests")
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private UpdateProductStatusRequest validRequest;
    private UpdateProductStatusResponse successResponse;

    @BeforeEach
    void setUp() {
        validRequest = UpdateProductStatusRequest.builder()
                .status("Banned")
                .build();

        successResponse = UpdateProductStatusResponse.builder()
                .productId(1L)
                .status("Banned")
                .message("Product status updated successfully")
                .build();
    }

    @Test
    @DisplayName("Should update product status successfully (200 OK)")
    @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
    void updateProductStatus_Success() throws Exception {
        // Given
        when(productService.updateProductStatus(eq(1L), any(UpdateProductStatusRequest.class)))
                .thenReturn(successResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/status", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.status").value("Banned"))
                .andExpect(jsonPath("$.message").value("Product status updated successfully"));

        verify(productService).updateProductStatus(eq(1L), any(UpdateProductStatusRequest.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails (invalid status)")
    @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
    void updateProductStatus_ValidationError() throws Exception {
        // Given
        UpdateProductStatusRequest invalidRequest = UpdateProductStatusRequest.builder()
                .status("InvalidStatus")
                .build();

        // When & Then
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/status", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 Not Found when product does not exist")
    @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
    void updateProductStatus_ProductNotFound() throws Exception {
        // Given
        when(productService.updateProductStatus(eq(999L), any(UpdateProductStatusRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/status", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user is not an ADMIN")
    @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
    void updateProductStatus_WrongRole() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/status", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user is not authenticated")
    void updateProductStatus_Unauthenticated() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/v1/admin/products/{productId}/status", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }
}
