package com.example.DACN.controller;

import com.example.DACN.dto.response.DeleteProductResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductController - Delete Product Tests")
class DeleteProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private DeleteProductResponse deleteResponse;

    @BeforeEach
    void setUp() {
        deleteResponse = DeleteProductResponse.builder()
                .productId(1L)
                .message("Product deleted successfully")
                .build();
    }

    // Validation Tests

    @Test
    @DisplayName("Should return 403 when user not authenticated")
    void testDeleteProductUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isForbidden());

        verify(productService, never()).deleteProduct(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should return 403 when user is not a seller")
    @WithMockUser(username = "customer@example.com", roles = { "CUSTOMER" })
    void testDeleteProductForbiddenForCustomer() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isForbidden());

        verify(productService, never()).deleteProduct(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductNotFound() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    @DisplayName("Should return 403 when product doesn't belong to seller")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductNotOwner() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong()))
                .thenThrow(new UnauthorizedException("You can only delete your own products"));

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own products"));
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should return 200 OK when product deleted successfully")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductSuccess() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong())).thenReturn(deleteResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));

        verify(productService).deleteProduct(eq("seller@example.com"), eq(1L));
    }

    // Response Structure Tests

    @Test
    @DisplayName("Should verify response structure contains all required fields")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductResponseStructure() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong())).thenReturn(deleteResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return correct content type")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductContentType() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong())).thenReturn(deleteResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    // Error Handling Tests

    @Test
    @DisplayName("Should return 403 when user is not seller")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductNotSellerInService() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong()))
                .thenThrow(new UnauthorizedException("Only sellers can delete products"));

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only sellers can delete products"));
    }

    @Test
    @DisplayName("Should return 404 when seller has no shop")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductSellerHasNoShop() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Seller does not have a shop"));

        // When & Then
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Seller does not have a shop"));
    }

    @Test
    @DisplayName("Should verify service is called exactly once")
    @WithMockUser(username = "seller@example.com", roles = { "SELLER" })
    void testDeleteProductServiceCalledOnce() throws Exception {
        // Given
        when(productService.deleteProduct(anyString(), anyLong())).thenReturn(deleteResponse);

        // When
        mockMvc.perform(delete("/api/v1/seller/products/1"))
                .andExpect(status().isOk());

        // Then
        verify(productService, times(1)).deleteProduct(anyString(), anyLong());
    }
}
