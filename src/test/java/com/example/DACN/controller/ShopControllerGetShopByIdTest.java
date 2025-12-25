package com.example.DACN.controller;

import com.example.DACN.dto.response.ShopDetailResponse;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.service.ShopService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ShopController - Get Shop By ID Tests")
class ShopControllerGetShopByIdTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShopService shopService;

    private ShopDetailResponse shopDetailResponse;

    @BeforeEach
    void setUp() {
        shopDetailResponse = ShopDetailResponse.builder()
                .shopId(1L)
                .shopName("Test Shop")
                .shopDescription("Test shop description")
                .logoUrl("https://cloudinary.com/logo.png")
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Should return 200 with shop details")
    void testGetShopByIdSuccess() throws Exception {
        // Given
        Long shopId = 1L;
        when(shopService.getShopById(shopId)).thenReturn(shopDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopId").value(1))
                .andExpect(jsonPath("$.shopName").value("Test Shop"))
                .andExpect(jsonPath("$.shopDescription").value("Test shop description"))
                .andExpect(jsonPath("$.logoUrl").value("https://cloudinary.com/logo.png"))
                .andExpect(jsonPath("$.ratingAvg").value(4.5))
                .andExpect(jsonPath("$.isApproved").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(shopService).getShopById(shopId);
    }

    @Test
    @DisplayName("Should return 404 when shop not found")
    void testGetShopByIdNotFound() throws Exception {
        // Given
        Long shopId = 999L;
        when(shopService.getShopById(shopId)).thenThrow(new ResourceNotFoundException("Shop not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isNotFound());

        verify(shopService).getShopById(shopId);
    }

    @Test
    @DisplayName("Should allow public access without authentication")
    void testGetShopByIdPublicAccess() throws Exception {
        // Given
        Long shopId = 1L;
        when(shopService.getShopById(shopId)).thenReturn(shopDetailResponse);

        // When & Then - No authentication required
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isOk());

        verify(shopService).getShopById(shopId);
    }

    @Test
    @DisplayName("Should verify response structure contains all required fields")
    void testGetShopByIdResponseStructure() throws Exception {
        // Given
        Long shopId = 1L;
        when(shopService.getShopById(shopId)).thenReturn(shopDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopId").exists())
                .andExpect(jsonPath("$.shopName").exists())
                .andExpect(jsonPath("$.shopDescription").exists())
                .andExpect(jsonPath("$.logoUrl").exists())
                .andExpect(jsonPath("$.ratingAvg").exists())
                .andExpect(jsonPath("$.isApproved").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetShopByIdContentType() throws Exception {
        // Given
        Long shopId = 1L;
        when(shopService.getShopById(shopId)).thenReturn(shopDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should verify service is called exactly once")
    void testGetShopByIdServiceCalledOnce() throws Exception {
        // Given
        Long shopId = 1L;
        when(shopService.getShopById(shopId)).thenReturn(shopDetailResponse);

        // When
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isOk());

        // Then
        verify(shopService, times(1)).getShopById(shopId);
    }

    @Test
    @DisplayName("Should handle shop with null optional fields")
    void testGetShopByIdWithNullOptionalFields() throws Exception {
        // Given
        Long shopId = 1L;
        ShopDetailResponse responseWithNulls = ShopDetailResponse.builder()
                .shopId(1L)
                .shopName("Test Shop")
                .shopDescription(null)
                .logoUrl(null)
                .ratingAvg(BigDecimal.ZERO)
                .isApproved(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(shopService.getShopById(shopId)).thenReturn(responseWithNulls);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopId").value(1))
                .andExpect(jsonPath("$.shopName").value("Test Shop"))
                .andExpect(jsonPath("$.shopDescription").isEmpty())
                .andExpect(jsonPath("$.logoUrl").isEmpty())
                .andExpect(jsonPath("$.ratingAvg").value(0))
                .andExpect(jsonPath("$.isApproved").value(true));
    }

    @Test
    @DisplayName("Should handle different shop IDs correctly")
    void testGetShopByIdDifferentIds() throws Exception {
        // Given
        Long shopId1 = 1L;
        Long shopId2 = 2L;

        ShopDetailResponse response1 = ShopDetailResponse.builder()
                .shopId(1L)
                .shopName("Shop 1")
                .shopDescription("Description 1")
                .logoUrl("https://cloudinary.com/logo1.png")
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ShopDetailResponse response2 = ShopDetailResponse.builder()
                .shopId(2L)
                .shopName("Shop 2")
                .shopDescription("Description 2")
                .logoUrl("https://cloudinary.com/logo2.png")
                .ratingAvg(new BigDecimal("4.8"))
                .isApproved(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(shopService.getShopById(shopId1)).thenReturn(response1);
        when(shopService.getShopById(shopId2)).thenReturn(response2);

        // When & Then
        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopId").value(1))
                .andExpect(jsonPath("$.shopName").value("Shop 1"));

        mockMvc.perform(get("/api/v1/shops/{shop_id}", shopId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopId").value(2))
                .andExpect(jsonPath("$.shopName").value("Shop 2"));

        verify(shopService).getShopById(shopId1);
        verify(shopService).getShopById(shopId2);
    }
}
