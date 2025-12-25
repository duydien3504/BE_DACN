package com.example.DACN.controller;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PublicCategoryController - Get Root Categories Tests")
class PublicCategoryControllerGetRootsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("Should return 200 with root categories for public access")
    void testGetRootCategoriesSuccess() throws Exception {
        // Given
        List<CategoryResponse> roots = List.of(
                CategoryResponse.builder().categoryId(1L).name("Electronics").slug("electronics").build(),
                CategoryResponse.builder().categoryId(2L).name("Fashion").slug("fashion").build());
        when(categoryService.getRootCategories()).thenReturn(roots);

        // When & Then
        mockMvc.perform(get("/api/v1/categories/roots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[1].categoryId").value(2));

        verify(categoryService).getRootCategories();
    }

    @Test
    @DisplayName("Should return empty array when no root categories")
    void testGetRootCategoriesEmpty() throws Exception {
        // Given
        when(categoryService.getRootCategories()).thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/api/v1/categories/roots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(categoryService).getRootCategories();
    }

    @Test
    @DisplayName("Should be accessible without authentication")
    void testGetRootCategoriesPublicAccess() throws Exception {
        // Given
        when(categoryService.getRootCategories()).thenReturn(new ArrayList<>());

        // When & Then - No authentication required
        mockMvc.perform(get("/api/v1/categories/roots"))
                .andExpect(status().isOk());

        verify(categoryService).getRootCategories();
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetRootCategoriesContentType() throws Exception {
        // Given
        when(categoryService.getRootCategories()).thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/api/v1/categories/roots"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}
