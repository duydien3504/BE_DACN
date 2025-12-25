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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CategoryController - Create Category Tests")
class CategoryControllerCreateTest {

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
                .name("Electronics")
                .slug("electronics")
                .iconUrl("https://cloudinary.com/icon.png")
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
    @DisplayName("Should return 201 when category created successfully")
    void testCreateCategorySuccess() throws Exception {
        // Given
        when(categoryService.createCategory(any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"))
                .andExpect(jsonPath("$.iconUrl").value("https://cloudinary.com/icon.png"))
                .andExpect(jsonPath("$.parentId").isEmpty())
                .andExpect(jsonPath("$.hasDeleted").value(false));

        verify(categoryService).createCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 201 when creating category without icon")
    void testCreateCategoryWithoutIcon() throws Exception {
        // Given
        categoryResponse.setIconUrl(null);
        when(categoryService.createCategory(any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iconUrl").isEmpty());

        verify(categoryService).createCategory(any(), isNull());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 201 when creating subcategory with parent")
    void testCreateSubcategorySuccess() throws Exception {
        // Given
        categoryResponse.setParentId(10L);
        when(categoryService.createCategory(any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics")
                .param("parentId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(10));

        verify(categoryService).createCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 when slug already exists")
    void testCreateCategoryDuplicateSlug() throws Exception {
        // Given
        when(categoryService.createCategory(any(), any()))
                .thenThrow(new DuplicateResourceException("Category with slug 'electronics' already exists"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isConflict());

        verify(categoryService).createCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when parent category not found")
    void testCreateCategoryParentNotFound() throws Exception {
        // Given
        when(categoryService.createCategory(any(), any()))
                .thenThrow(new ResourceNotFoundException("Parent category not found"));

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics")
                .param("parentId", "999"))
                .andExpect(status().isNotFound());

        verify(categoryService).createCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should return 403 when user is not admin")
    void testCreateCategoryForbidden() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any(), any());
    }

    @Test
    @DisplayName("Should return 403 when user is not authenticated")
    void testCreateCategoryUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should verify response structure")
    void testCreateCategoryResponseStructure() throws Exception {
        // Given
        when(categoryService.createCategory(any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.slug").exists())
                .andExpect(jsonPath("$.hasDeleted").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return correct content type")
    void testCreateCategoryContentType() throws Exception {
        // Given
        when(categoryService.createCategory(any(), any())).thenReturn(categoryResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should verify service is called exactly once")
    void testCreateCategoryServiceCalledOnce() throws Exception {
        // Given
        when(categoryService.createCategory(any(), any())).thenReturn(categoryResponse);

        // When
        mockMvc.perform(multipart("/api/v1/admin/categories")
                .file(iconFile)
                .param("name", "Electronics")
                .param("slug", "electronics"))
                .andExpect(status().isCreated());

        // Then
        verify(categoryService, times(1)).createCategory(any(), any());
    }
}
