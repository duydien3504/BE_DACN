package com.example.DACN.controller;

import com.example.DACN.dto.response.UpdateProductResponse;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductController - Update Product Tests")
class UpdateProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private UpdateProductResponse updateResponse;
    private MockMultipartFile image1;
    private MockMultipartFile image2;

    @BeforeEach
    void setUp() {
        updateResponse = UpdateProductResponse.builder()
                .productId(1L)
                .message("Product updated successfully")
                .build();

        image1 = new MockMultipartFile(
                "images",
                "test1.jpg",
                "image/jpeg",
                "test image content".getBytes());

        image2 = new MockMultipartFile(
                "images",
                "test2.png",
                "image/png",
                "test image content".getBytes());
    }

    // Validation Tests

    @Test
    @DisplayName("Should return 403 when user not authenticated")
    void testUpdateProductUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateProduct(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should return 403 when user is not a seller")
    @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
    void testUpdateProductForbiddenForCustomer() throws Exception {
        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateProduct(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductNotFound() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/999")
                .param("name", "Updated Product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    @DisplayName("Should return 403 when product doesn't belong to seller")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductNotOwner() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any()))
                .thenThrow(new UnauthorizedException("You can only update your own products"));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your own products"));
    }

    @Test
    @DisplayName("Should return 400 when price is negative")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductInvalidPrice() throws Exception {
        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("price", "-10.00"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).updateProduct(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should return 400 when stock quantity is negative")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductInvalidStockQuantity() throws Exception {
        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("stockQuantity", "-5"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).updateProduct(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should return 404 when category not found")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductCategoryNotFound() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("categoryId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    @DisplayName("Should return 400 when more than 9 images")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductTooManyImages() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), anyList()))
                .thenThrow(new IllegalArgumentException("Maximum 9 images allowed"));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .file(image1)
                .file(image2)
                .param("name", "Updated Product"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum 9 images allowed"));
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should return 200 OK when product updated successfully")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductSuccess() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product")
                .param("description", "Updated Description")
                .param("price", "149.99")
                .param("categoryId", "2")
                .param("stockQuantity", "20")
                .param("status", "Active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.message").value("Product updated successfully"));

        verify(productService).updateProduct(eq("seller@example.com"), eq(1L), any(), any());
    }

    @Test
    @DisplayName("Should update product with partial fields")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductPartialUpdate() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product Name Only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));

        verify(productService).updateProduct(eq("seller@example.com"), eq(1L),
                argThat(request -> request.getName().equals("Updated Product Name Only")
                        && request.getPrice() == null
                        && request.getCategoryId() == null),
                any());
    }

    @Test
    @DisplayName("Should update product with new images")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductWithNewImages() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), anyList())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .file(image1)
                .file(image2)
                .param("name", "Updated Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));

        verify(productService).updateProduct(eq("seller@example.com"), eq(1L), any(), anyList());
    }

    @Test
    @DisplayName("Should update product without changing images")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductWithoutImages() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), isNull())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product")
                .param("price", "199.99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));

        verify(productService).updateProduct(eq("seller@example.com"), eq(1L), any(), isNull());
    }

    @Test
    @DisplayName("Should update product status")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductStatus() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("status", "Inactive"))
                .andExpect(status().isOk());

        verify(productService).updateProduct(eq("seller@example.com"), eq(1L),
                argThat(request -> request.getStatus().equals("Inactive")),
                any());
    }

    // Response Structure Tests

    @Test
    @DisplayName("Should verify response structure contains all required fields")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductResponseStructure() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return correct content type")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductContentType() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any())).thenReturn(updateResponse);

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    // Error Handling Tests

    @Test
    @DisplayName("Should return 403 when user is not seller")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductNotSellerInService() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any()))
                .thenThrow(new UnauthorizedException("Only sellers can update products"));

        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only sellers can update products"));
    }

    @Test
    @DisplayName("Should verify service is called exactly once")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductServiceCalledOnce() throws Exception {
        // Given
        when(productService.updateProduct(anyString(), anyLong(), any(), any())).thenReturn(updateResponse);

        // When
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("name", "Updated Product"))
                .andExpect(status().isOk());

        // Then
        verify(productService, times(1)).updateProduct(anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should handle invalid status value")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testUpdateProductInvalidStatus() throws Exception {
        // When & Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/seller/products/1")
                .param("status", "InvalidStatus"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).updateProduct(anyString(), anyLong(), any(), any());
    }
}
