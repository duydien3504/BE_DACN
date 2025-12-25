package com.example.DACN.controller;

import com.example.DACN.dto.response.ProductListItemResponse;
import com.example.DACN.dto.response.ProductListResponse;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PublicShopController - Get Shop Products Tests")
class ShopProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductListResponse productListResponse;
    private ProductListItemResponse product1;
    private ProductListItemResponse product2;

    @BeforeEach
    void setUp() {
        product1 = ProductListItemResponse.builder()
                .productId(1L)
                .name("Product 1")
                .description("Description 1")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .soldCount(5)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .shopId(1L)
                .shopName("Test Shop")
                .categoryId(1L)
                .categoryName("Category 1")
                .build();

        product2 = ProductListItemResponse.builder()
                .productId(2L)
                .name("Product 2")
                .description("Description 2")
                .price(new BigDecimal("149.99"))
                .stockQuantity(20)
                .soldCount(10)
                .status("Active")
                .createdAt(LocalDateTime.now())
                .shopId(1L)
                .shopName("Test Shop")
                .categoryId(2L)
                .categoryName("Category 2")
                .build();

        productListResponse = ProductListResponse.builder()
                .data(Arrays.asList(product1, product2))
                .totalPage(1)
                .build();
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should return 200 OK with shop products")
    void testGetShopProductsSuccess() throws Exception {
        // Given
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalPage").value(1))
                .andExpect(jsonPath("$.data[0].shopId").value(1))
                .andExpect(jsonPath("$.data[1].shopId").value(1));

        verify(productService).getProductsByShopId(1L, 0, 20);
    }

    @Test
    @DisplayName("Should support pagination")
    void testGetShopProductsWithPagination() throws Exception {
        // Given
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());

        verify(productService).getProductsByShopId(1L, 1, 10);
    }

    @Test
    @DisplayName("Should use default pagination values")
    void testGetShopProductsDefaultPagination() throws Exception {
        // Given
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk());

        verify(productService).getProductsByShopId(1L, 0, 20);
    }

    // Validation Tests

    @Test
    @DisplayName("Should return 400 when page is negative")
    void testGetShopProductsInvalidPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products")
                .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getProductsByShopId(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should return 400 when size is zero")
    void testGetShopProductsInvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products")
                .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getProductsByShopId(anyLong(), anyInt(), anyInt());
    }

    // Response Structure Tests

    @Test
    @DisplayName("Should verify response structure")
    void testGetShopProductsResponseStructure() throws Exception {
        // Given
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.totalPage").exists())
                .andExpect(jsonPath("$.data[0].productId").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].shopId").exists());
    }

    @Test
    @DisplayName("Should return empty list when shop has no products")
    void testGetShopProductsEmptyList() throws Exception {
        // Given
        ProductListResponse emptyResponse = ProductListResponse.builder()
                .data(Collections.emptyList())
                .totalPage(1)
                .build();
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(emptyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalPage").value(1));
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetShopProductsContentType() throws Exception {
        // Given
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should verify service is called exactly once")
    void testGetShopProductsServiceCalledOnce() throws Exception {
        // Given
        when(productService.getProductsByShopId(anyLong(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When
        mockMvc.perform(get("/api/v1/shops/1/products"))
                .andExpect(status().isOk());

        // Then
        verify(productService, times(1)).getProductsByShopId(anyLong(), anyInt(), anyInt());
    }
}
