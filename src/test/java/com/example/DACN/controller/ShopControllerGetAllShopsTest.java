package com.example.DACN.controller;

import com.example.DACN.dto.response.ShopListResponse;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ShopController - Get All Shops Tests")
class ShopControllerGetAllShopsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShopService shopService;

    private List<ShopListResponse> shopListResponses;

    @BeforeEach
    void setUp() {
        ShopListResponse response1 = ShopListResponse.builder()
                .shopId(1L)
                .shopName("Shop One")
                .shopDescription("First shop")
                .logoUrl("https://cloudinary.com/logo1.png")
                .ratingAvg(new BigDecimal("4.5"))
                .isApproved(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        ShopListResponse response2 = ShopListResponse.builder()
                .shopId(2L)
                .shopName("Shop Two")
                .shopDescription("Second shop")
                .logoUrl("https://cloudinary.com/logo2.png")
                .ratingAvg(new BigDecimal("4.8"))
                .isApproved(true)
                .createdAt(LocalDateTime.now().minusDays(20))
                .build();

        shopListResponses = Arrays.asList(response1, response2);
    }

    @Test
    @DisplayName("Should return 200 with list of shops")
    void testGetAllShopsSuccess() throws Exception {
        // Given
        when(shopService.getAllShops()).thenReturn(shopListResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shopId").value(1))
                .andExpect(jsonPath("$[0].shopName").value("Shop One"))
                .andExpect(jsonPath("$[0].shopDescription").value("First shop"))
                .andExpect(jsonPath("$[0].ratingAvg").value(4.5))
                .andExpect(jsonPath("$[0].isApproved").value(true))
                .andExpect(jsonPath("$[1].shopId").value(2))
                .andExpect(jsonPath("$[1].shopName").value("Shop Two"));

        verify(shopService).getAllShops();
    }

    @Test
    @DisplayName("Should return 200 with empty array when no shops")
    void testGetAllShopsEmptyList() throws Exception {
        // Given
        when(shopService.getAllShops()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(shopService).getAllShops();
    }

    @Test
    @DisplayName("Should allow public access without authentication")
    void testGetAllShopsPublicAccess() throws Exception {
        // Given
        when(shopService.getAllShops()).thenReturn(shopListResponses);

        // When & Then - No authentication required
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk());

        verify(shopService).getAllShops();
    }

    @Test
    @DisplayName("Should verify response structure contains all required fields")
    void testGetAllShopsResponseStructure() throws Exception {
        // Given
        when(shopService.getAllShops()).thenReturn(shopListResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shopId").exists())
                .andExpect(jsonPath("$[0].shopName").exists())
                .andExpect(jsonPath("$[0].shopDescription").exists())
                .andExpect(jsonPath("$[0].logoUrl").exists())
                .andExpect(jsonPath("$[0].ratingAvg").exists())
                .andExpect(jsonPath("$[0].isApproved").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetAllShopsContentType() throws Exception {
        // Given
        when(shopService.getAllShops()).thenReturn(shopListResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should verify service is called exactly once")
    void testGetAllShopsServiceCalledOnce() throws Exception {
        // Given
        when(shopService.getAllShops()).thenReturn(shopListResponses);

        // When
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk());

        // Then
        verify(shopService, times(1)).getAllShops();
    }

    @Test
    @DisplayName("Should handle shops with null optional fields")
    void testGetAllShopsWithNullOptionalFields() throws Exception {
        // Given
        ShopListResponse responseWithNulls = ShopListResponse.builder()
                .shopId(1L)
                .shopName("Shop One")
                .shopDescription(null)
                .logoUrl(null)
                .ratingAvg(BigDecimal.ZERO)
                .isApproved(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(shopService.getAllShops()).thenReturn(Arrays.asList(responseWithNulls));

        // When & Then
        mockMvc.perform(get("/api/v1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shopId").value(1))
                .andExpect(jsonPath("$[0].shopName").value("Shop One"))
                .andExpect(jsonPath("$[0].shopDescription").isEmpty())
                .andExpect(jsonPath("$[0].logoUrl").isEmpty())
                .andExpect(jsonPath("$[0].ratingAvg").value(0))
                .andExpect(jsonPath("$[0].isApproved").value(true));
    }
}
