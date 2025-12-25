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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PublicProductController - Get Products Tests")
class PublicProductControllerTest {

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
                .shopName("Shop 1")
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
                .shopId(2L)
                .shopName("Shop 2")
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
    @DisplayName("Should return 200 OK with products list")
    void testGetProductsSuccess() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.totalPage").value(1));

        verify(productService).getProducts(null, null, null, null, "created_at", 0, 20);
    }

    @Test
    @DisplayName("Should filter by price range")
    void testGetProductsWithPriceFilter() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("minPrice", "50.00")
                .param("maxPrice", "150.00"))
                .andExpect(status().isOk());

        verify(productService).getProducts(
                eq(new BigDecimal("50.00")),
                eq(new BigDecimal("150.00")),
                isNull(),
                isNull(),
                eq("created_at"),
                eq(0),
                eq(20));
    }

    @Test
    @DisplayName("Should filter by category")
    void testGetProductsWithCategoryFilter() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("category_id", "1"))
                .andExpect(status().isOk());

        verify(productService).getProducts(null, null, 1L, null, "created_at", 0, 20);
    }

    @Test
    @DisplayName("Should filter by shop")
    void testGetProductsWithShopFilter() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("shop_id", "1"))
                .andExpect(status().isOk());

        verify(productService).getProducts(null, null, null, 1L, "created_at", 0, 20);
    }

    @Test
    @DisplayName("Should sort by sold_count")
    void testGetProductsSortBySoldCount() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("sort_by", "sold_count"))
                .andExpect(status().isOk());

        verify(productService).getProducts(null, null, null, null, "sold_count", 0, 20);
    }

    @Test
    @DisplayName("Should sort by created_at by default")
    void testGetProductsDefaultSort() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());

        verify(productService).getProducts(null, null, null, null, "created_at", 0, 20);
    }

    @Test
    @DisplayName("Should support pagination")
    void testGetProductsWithPagination() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());

        verify(productService).getProducts(null, null, null, null, "created_at", 1, 10);
    }

    @Test
    @DisplayName("Should combine multiple filters")
    void testGetProductsWithMultipleFilters() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("minPrice", "50.00")
                .param("maxPrice", "200.00")
                .param("category_id", "1")
                .param("shop_id", "2")
                .param("sort_by", "sold_count")
                .param("page", "0")
                .param("size", "15"))
                .andExpect(status().isOk());

        verify(productService).getProducts(
                eq(new BigDecimal("50.00")),
                eq(new BigDecimal("200.00")),
                eq(1L),
                eq(2L),
                eq("sold_count"),
                eq(0),
                eq(15));
    }

    // Validation Tests

    @Test
    @DisplayName("Should return 400 when page is negative")
    void testGetProductsInvalidPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should return 400 when size is zero")
    void testGetProductsInvalidSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/products")
                .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt());
    }

    // Response Structure Tests

    @Test
    @DisplayName("Should verify response structure")
    void testGetProductsResponseStructure() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.totalPage").exists())
                .andExpect(jsonPath("$.data[0].productId").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].price").exists())
                .andExpect(jsonPath("$.data[0].shopId").exists())
                .andExpect(jsonPath("$.data[0].categoryId").exists());
    }

    @Test
    @DisplayName("Should return empty list when no products found")
    void testGetProductsEmptyList() throws Exception {
        // Given
        ProductListResponse emptyResponse = ProductListResponse.builder()
                .data(Collections.emptyList())
                .totalPage(0)
                .build();
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(emptyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.totalPage").value(0));
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetProductsContentType() throws Exception {
        // Given
        when(productService.getProducts(any(), any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(productListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}
