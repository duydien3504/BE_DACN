package com.example.DACN.controller;

import com.example.DACN.dto.response.MyShopResponse;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.service.ShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ShopController - Get My Shop Tests")
class ShopControllerGetMyShopTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ShopService shopService;

        private MyShopResponse myShopResponse;

        @BeforeEach
        void setUp() {
                myShopResponse = MyShopResponse.builder()
                                .shopId(1L)
                                .shopName("My Awesome Shop")
                                .shopDescription("Best products in town")
                                .logoUrl("https://cloudinary.com/logo.png")
                                .ratingAvg(new BigDecimal("4.5"))
                                .isApproved(true)
                                .createdAt(LocalDateTime.now().minusDays(30))
                                .updatedAt(LocalDateTime.now())
                                .build();
        }

        @Test
        @DisplayName("Should return 200 with shop data for authenticated seller")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testGetMyShopSuccess() throws Exception {
                // Given
                when(shopService.getMyShop("seller@example.com")).thenReturn(myShopResponse);

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.shopId").value(1))
                                .andExpect(jsonPath("$.shopName").value("My Awesome Shop"))
                                .andExpect(jsonPath("$.shopDescription").value("Best products in town"))
                                .andExpect(jsonPath("$.logoUrl").value("https://cloudinary.com/logo.png"))
                                .andExpect(jsonPath("$.ratingAvg").value(4.5))
                                .andExpect(jsonPath("$.isApproved").value(true))
                                .andExpect(jsonPath("$.createdAt").exists())
                                .andExpect(jsonPath("$.updatedAt").exists());

                verify(shopService).getMyShop("seller@example.com");
        }

        @Test
        @DisplayName("Should return 403 when user is not authenticated")
        void testGetMyShopUnauthorized() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isForbidden());

                verify(shopService, never()).getMyShop(any());
        }

        @Test
        @DisplayName("Should return 403 when user is not a seller")
        @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
        void testGetMyShopForbiddenForCustomer() throws Exception {
                // Given
                when(shopService.getMyShop("customer@example.com"))
                                .thenThrow(new UnauthorizedException("Only sellers can access shop information"));

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Only sellers can access shop information"));

                verify(shopService).getMyShop("customer@example.com");
        }

        @Test
        @DisplayName("Should return 403 when admin tries to access")
        @WithMockUser(username = "admin@example.com", roles = { "ADMIN" })
        void testGetMyShopForbiddenForAdmin() throws Exception {
                // Given
                when(shopService.getMyShop("admin@example.com"))
                                .thenThrow(new UnauthorizedException("Only sellers can access shop information"));

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Only sellers can access shop information"));

                verify(shopService).getMyShop("admin@example.com");
        }

        @Test
        @DisplayName("Should return 404 when seller has no shop")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testGetMyShopNotFound() throws Exception {
                // Given
                when(shopService.getMyShop("seller@example.com"))
                                .thenThrow(new ResourceNotFoundException("Shop not found"));

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Shop not found"));

                verify(shopService).getMyShop("seller@example.com");
        }

        @Test
        @DisplayName("Should verify response structure contains all required fields")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testGetMyShopResponseStructure() throws Exception {
                // Given
                when(shopService.getMyShop("seller@example.com")).thenReturn(myShopResponse);

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
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
        @DisplayName("Should handle shop with null optional fields")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testGetMyShopWithNullOptionalFields() throws Exception {
                // Given
                MyShopResponse responseWithNulls = MyShopResponse.builder()
                                .shopId(1L)
                                .shopName("My Shop")
                                .shopDescription(null)
                                .logoUrl(null)
                                .ratingAvg(BigDecimal.ZERO)
                                .isApproved(false)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                when(shopService.getMyShop("seller@example.com")).thenReturn(responseWithNulls);

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.shopId").value(1))
                                .andExpect(jsonPath("$.shopName").value("My Shop"))
                                .andExpect(jsonPath("$.shopDescription").isEmpty())
                                .andExpect(jsonPath("$.logoUrl").isEmpty())
                                .andExpect(jsonPath("$.ratingAvg").value(0))
                                .andExpect(jsonPath("$.isApproved").value(false));
        }

        @Test
        @DisplayName("Should return correct content type")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testGetMyShopContentType() throws Exception {
                // Given
                when(shopService.getMyShop("seller@example.com")).thenReturn(myShopResponse);

                // When & Then
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("Should verify service is called exactly once")
        @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
        void testGetMyShopServiceCalledOnce() throws Exception {
                // Given
                when(shopService.getMyShop("seller@example.com")).thenReturn(myShopResponse);

                // When
                mockMvc.perform(get("/api/v1/shops/my-shop"))
                                .andExpect(status().isOk());

                // Then
                verify(shopService, times(1)).getMyShop("seller@example.com");
        }
}
