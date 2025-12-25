package com.example.DACN.controller;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.exception.DuplicateResourceException;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CategoryController - Update Category Tests")
class CategoryControllerUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private CategoryResponse categoryResponse;
    private MockMultipartFile iconFile;

    @BeforeEach
    void setUp() {
        categoryResponse = CategoryResponse.builder()
                .categoryId(1L)
                .name("Updated Category")
                .slug("updated-category")
                .iconUrl("https://cloudinary.com/new-icon.png")
                .parentId(null)
                .hasDeleted(false)
                .build();

        iconFile = new MockMultipartFile(
                "icon",
                "icon.png",
                "image/png",
                "test image content".getBytes());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 when category updated successfully")
    void testUpdateCategorySuccess() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .file(iconFile)
                .param("name", "Updated Category")
                .param("slug", "updated-category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.name").value("Updated Category"))
                .andExpect(jsonPath("$.slug").value("updated-category"))
                .andExpect(jsonPath("$.iconUrl").value("https://cloudinary.com/new-icon.png"));

        verify(categoryService).updateCategory(eq(1L), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 when updating only name")
    void testUpdateCategoryNameOnly() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());

        verify(categoryService).updateCategory(eq(1L), any(), isNull());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 when updating with parent")
    void testUpdateCategoryWithParent() throws Exception {
        // Given
        categoryResponse.setParentId(10L);
        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .param("parentId", "10")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(10));

        verify(categoryService).updateCategory(eq(1L), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when category not found")
    void testUpdateCategoryNotFound() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/999")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isNotFound());

        verify(categoryService).updateCategory(eq(999L), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 when slug already exists")
    void testUpdateCategoryDuplicateSlug() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(), any()))
                .thenThrow(new DuplicateResourceException("Category with slug 'updated-category' already exists"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("slug", "updated-category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isConflict());

        verify(categoryService).updateCategory(eq(1L), any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should return 403 when user is not admin")
    void testUpdateCategoryForbidden() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).updateCategory(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 403 when user is not authenticated")
    void testUpdateCategoryUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).updateCategory(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should verify response structure")
    void testUpdateCategoryResponseStructure() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.slug").exists())
                .andExpect(jsonPath("$.hasDeleted").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return correct content type")
    void testUpdateCategoryContentType() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should verify service is called exactly once")
    void testUpdateCategoryServiceCalledOnce() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(), any())).thenReturn(categoryResponse);

        // When
        mockMvc.perform(multipart("/api/v1/admin/categories/1")
                .param("name", "Updated Category")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());

        // Then
        verify(categoryService, times(1)).updateCategory(eq(1L), any(), any());
    }
}
