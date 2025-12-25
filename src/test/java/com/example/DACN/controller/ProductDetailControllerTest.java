package com.example.DACN.controller;

import com.example.DACN.dto.response.ProductDetailResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PublicProductController - Get Product By ID Tests")
class ProductDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDetailResponse productDetailResponse;

    @BeforeEach
    void setUp() {
        productDetailResponse = ProductDetailResponse.builder()
                .productId(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .soldCount(5)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .shopId(1L)
                .shopName("Test Shop")
                .shopDescription("Test Shop Description")
                .categoryId(1L)
                .categoryName("Test Category")
                .images(Arrays.asList("https://example.com/image1.jpg", "https://example.com/image2.jpg"))
                .build();
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should return 200 OK with product details")
    void testGetProductByIdSuccess() throws Exception {
        // Given
        when(productService.getProductById(anyLong())).thenReturn(productDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.shopId").value(1))
                .andExpect(jsonPath("$.shopName").value("Test Shop"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(2));

        verify(productService).getProductById(1L);
    }

    // Error Handling Tests

    @Test
    @DisplayName("Should return 404 when product not found")
    void testGetProductByIdNotFound() throws Exception {
        // Given
        when(productService.getProductById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));

        verify(productService).getProductById(999L);
    }

    // Response Structure Tests

    @Test
    @DisplayName("Should verify response structure contains all required fields")
    void testGetProductByIdResponseStructure() throws Exception {
        // Given
        when(productService.getProductById(anyLong())).thenReturn(productDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.stockQuantity").exists())
                .andExpect(jsonPath("$.soldCount").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.shopId").exists())
                .andExpect(jsonPath("$.shopName").exists())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.categoryName").exists())
                .andExpect(jsonPath("$.images").exists());
    }

    @Test
    @DisplayName("Should return product with empty images list")
    void testGetProductByIdWithNoImages() throws Exception {
        // Given
        productDetailResponse.setImages(Collections.emptyList());
        when(productService.getProductById(anyLong())).thenReturn(productDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetProductByIdContentType() throws Exception {
        // Given
        when(productService.getProductById(anyLong())).thenReturn(productDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should verify service is called exactly once")
    void testGetProductByIdServiceCalledOnce() throws Exception {
        // Given
        when(productService.getProductById(anyLong())).thenReturn(productDetailResponse);

        // When
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk());

        // Then
        verify(productService, times(1)).getProductById(1L);
    }
}
