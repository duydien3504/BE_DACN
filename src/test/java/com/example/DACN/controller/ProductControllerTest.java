package com.example.DACN.controller;

import com.example.DACN.dto.response.CreateProductResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductController - Create Product Tests")
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ProductService productService;

        private CreateProductResponse response;
        private UpdateProductResponse updateResponse;
        private MockMultipartFile image1;
        private MockMultipartFile image2;

        @BeforeEach
        void setUp() {
                response = CreateProductResponse.builder()
                                .productId(1L)
                                .message("Product created successfully")
                                .build();

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
        void testCreateProductUnauthorized() throws Exception {
                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isForbidden());

                verify(productService, never()).createProduct(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 403 when user is not a seller")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void testCreateProductForbiddenForCustomer() throws Exception {
                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isForbidden());

                verify(productService, never()).createProduct(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductMissingName() throws Exception {
                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isBadRequest());

                verify(productService, never()).createProduct(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when price is missing")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductMissingPrice() throws Exception {
                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("categoryId", "1"))
                                .andExpect(status().isBadRequest());

                verify(productService, never()).createProduct(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when categoryId is missing")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductMissingCategoryId() throws Exception {
                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99"))
                                .andExpect(status().isBadRequest());

                verify(productService, never()).createProduct(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when more than 9 images")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductTooManyImages() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), anyList()))
                                .thenThrow(new IllegalArgumentException("Maximum 9 images allowed"));

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .file(image1)
                                .file(image2)
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Maximum 9 images allowed"));
        }

        @Test
        @DisplayName("Should return 400 when invalid image format")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductInvalidImageFormat() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), anyList()))
                                .thenThrow(new IllegalArgumentException(
                                                "Invalid file format. Only JPG and PNG are allowed for product images"));

                MockMultipartFile invalidImage = new MockMultipartFile(
                                "images",
                                "test.gif",
                                "image/gif",
                                "test image content".getBytes());

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .file(invalidImage)
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("Invalid file format. Only JPG and PNG are allowed for product images"));
        }

        // Core Logic Tests

        @Test
        @DisplayName("Should return 201 Created with valid request")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductSuccess() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("description", "Test Description")
                                .param("price", "99.99")
                                .param("categoryId", "1")
                                .param("stockQuantity", "10"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.productId").value(1))
                                .andExpect(jsonPath("$.message").value("Product created successfully"));

                verify(productService).createProduct(eq("seller@example.com"), any(), any());
        }

        @Test
        @DisplayName("Should create product successfully without images")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductSuccessWithoutImages() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), isNull())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.productId").value(1));

                verify(productService).createProduct(eq("seller@example.com"), any(), isNull());
        }

        @Test
        @DisplayName("Should create product successfully with multiple images")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductSuccessWithMultipleImages() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), anyList())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .file(image1)
                                .file(image2)
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.productId").value(1));

                verify(productService).createProduct(eq("seller@example.com"), any(), anyList());
        }

        @Test
        @DisplayName("Should use default stock quantity when not provided")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductDefaultStockQuantity() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated());

                verify(productService).createProduct(eq("seller@example.com"),
                                argThat(request -> request.getStockQuantity() == 0), any());
        }

        // Response Structure Tests

        @Test
        @DisplayName("Should verify response structure contains all required fields")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductResponseStructure() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.productId").exists())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return correct content type")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductContentType() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated())
                                .andExpect(content().contentType("application/json"));
        }

        // Error Handling Tests

        @Test
        @DisplayName("Should return 404 when category not found")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductCategoryNotFound() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any()))
                                .thenThrow(new ResourceNotFoundException("Category not found"));

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Category not found"));
        }

        @Test
        @DisplayName("Should return 404 when seller has no shop")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductSellerHasNoShop() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any()))
                                .thenThrow(new ResourceNotFoundException("Seller does not have a shop"));

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Seller does not have a shop"));
        }

        @Test
        @DisplayName("Should return 403 when shop is not approved")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductShopNotApproved() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any()))
                                .thenThrow(new IllegalStateException("Shop must be approved before creating products"));

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("Shop must be approved before creating products"));
        }

        @Test
        @DisplayName("Should return 403 when user is not a seller in service layer")
        @WithMockUser(username = "customer@example.com", roles = { "SELLER" })
        void testCreateProductNotSellerInService() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any()))
                                .thenThrow(new UnauthorizedException("Only sellers can create products"));

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Only sellers can create products"));
        }

        @Test
        @DisplayName("Should verify service is called exactly once")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductServiceCalledOnce() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any())).thenReturn(response);

                // When
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated());

                // Then
                verify(productService, times(1)).createProduct(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should handle optional description field")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testCreateProductWithoutDescription() throws Exception {
                // Given
                when(productService.createProduct(anyString(), any(), any())).thenReturn(response);

                // When & Then
                mockMvc.perform(multipart("/api/v1/seller/products")
                                .param("name", "Test Product")
                                .param("price", "99.99")
                                .param("categoryId", "1"))
                                .andExpect(status().isCreated());

                verify(productService).createProduct(eq("seller@example.com"),
                                argThat(request -> request.getDescription() == null), any());
        }
}
