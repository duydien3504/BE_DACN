package com.example.DACN.controller;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.service.CategoryService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CategoryController - Delete Category Tests")
class CategoryControllerDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryResponse = CategoryResponse.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .iconUrl(null)
                .parentId(null)
                .hasDeleted(true)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 when category deleted successfully")
    void testDeleteCategorySuccess() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(1L))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.hasDeleted").value(true));

        verify(categoryService).deleteCategory(eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when category not found")
    void testDeleteCategoryNotFound() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/999"))
                .andExpect(status().isNotFound());

        verify(categoryService).deleteCategory(eq(999L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when category already deleted")
    void testDeleteCategoryAlreadyDeleted() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(1L)))
                .thenThrow(new ResourceNotFoundException("Category has already been deleted"));

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isNotFound());

        verify(categoryService).deleteCategory(eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 when category has products")
    void testDeleteCategoryWithProducts() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(1L)))
                .thenThrow(new IllegalStateException("Cannot delete category with existing products"));

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isConflict());

        verify(categoryService).deleteCategory(eq(1L));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should return 403 when user is not admin")
    void testDeleteCategoryForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).deleteCategory(any());
    }

    @Test
    @DisplayName("Should return 403 when user is not authenticated")
    void testDeleteCategoryUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).deleteCategory(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should verify response structure")
    void testDeleteCategoryResponseStructure() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(1L))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.slug").exists())
                .andExpect(jsonPath("$.hasDeleted").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return correct content type")
    void testDeleteCategoryContentType() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(1L))).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should verify service is called exactly once")
    void testDeleteCategoryServiceCalledOnce() throws Exception {
        // Given
        when(categoryService.deleteCategory(eq(1L))).thenReturn(categoryResponse);

        // When
        mockMvc.perform(delete("/api/v1/admin/categories/1"))
                .andExpect(status().isOk());

        // Then
        verify(categoryService, times(1)).deleteCategory(eq(1L));
    }
}
