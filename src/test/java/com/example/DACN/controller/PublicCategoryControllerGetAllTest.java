package com.example.DACN.controller;

import com.example.DACN.dto.response.CategoryTreeResponse;
import com.example.DACN.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("PublicCategoryController - Get All Categories Tests")
class PublicCategoryControllerGetAllTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    private List<CategoryTreeResponse> categoryTree;

    @BeforeEach
    void setUp() {
        CategoryTreeResponse root = CategoryTreeResponse.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .hasDeleted(false)
                .children(List.of(
                        CategoryTreeResponse.builder()
                                .categoryId(2L)
                                .name("Phones")
                                .slug("phones")
                                .hasDeleted(false)
                                .children(null)
                                .build()))
                .build();

        categoryTree = List.of(root);
    }

    @Test
    @DisplayName("Should return 200 with category tree for public access")
    void testGetAllCategoriesSuccess() throws Exception {
        // Given
        when(categoryService.getAllCategoriesTree()).thenReturn(categoryTree);

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].children[0].categoryId").value(2));

        verify(categoryService).getAllCategoriesTree();
    }

    @Test
    @DisplayName("Should return empty array when no categories")
    void testGetAllCategoriesEmpty() throws Exception {
        // Given
        when(categoryService.getAllCategoriesTree()).thenReturn(new ArrayList<>());

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(categoryService).getAllCategoriesTree();
    }

    @Test
    @DisplayName("Should be accessible without authentication")
    void testGetAllCategoriesPublicAccess() throws Exception {
        // Given
        when(categoryService.getAllCategoriesTree()).thenReturn(categoryTree);

        // When & Then - No authentication required
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());

        verify(categoryService).getAllCategoriesTree();
    }

    @Test
    @DisplayName("Should return correct content type")
    void testGetAllCategoriesContentType() throws Exception {
        // Given
        when(categoryService.getAllCategoriesTree()).thenReturn(categoryTree);

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}
