package com.example.DACN.service;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.mapper.CategoryMapper;
import com.example.DACN.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Get Root Categories Tests")
class CategoryServiceGetRootsTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Should return empty list when no root categories exist")
    void testGetRootCategoriesEmpty() {
        // Given
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());

        // When
        List<CategoryResponse> result = categoryService.getRootCategories();

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should return only root categories (parent = null)")
    void testGetRootCategoriesSuccess() {
        // Given
        Category root1 = createCategory(1L, "Electronics", null, false);
        Category root2 = createCategory(2L, "Fashion", null, false);
        Category child = createCategory(3L, "Phones", root1, false);

        List<Category> allCategories = List.of(root1, root2, child);
        when(categoryRepository.findAll()).thenReturn(allCategories);

        CategoryResponse response1 = createResponse(1L, "Electronics");
        CategoryResponse response2 = createResponse(2L, "Fashion");

        when(categoryMapper.toCategoryResponse(root1)).thenReturn(response1);
        when(categoryMapper.toCategoryResponse(root2)).thenReturn(response2);

        // When
        List<CategoryResponse> result = categoryService.getRootCategories();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("categoryId").containsExactlyInAnyOrder(1L, 2L);
        verify(categoryMapper, times(2)).toCategoryResponse(any(Category.class));
    }

    @Test
    @DisplayName("Should exclude deleted root categories")
    void testGetRootCategoriesExcludeDeleted() {
        // Given
        Category activeRoot = createCategory(1L, "Electronics", null, false);
        Category deletedRoot = createCategory(2L, "Old Category", null, true);

        List<Category> allCategories = List.of(activeRoot, deletedRoot);
        when(categoryRepository.findAll()).thenReturn(allCategories);

        CategoryResponse response = createResponse(1L, "Electronics");
        when(categoryMapper.toCategoryResponse(activeRoot)).thenReturn(response);

        // When
        List<CategoryResponse> result = categoryService.getRootCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        verify(categoryMapper, times(1)).toCategoryResponse(any(Category.class));
    }

    @Test
    @DisplayName("Should exclude non-root categories (parent != null)")
    void testGetRootCategoriesExcludeChildren() {
        // Given
        Category root = createCategory(1L, "Electronics", null, false);
        Category child1 = createCategory(2L, "Phones", root, false);
        Category child2 = createCategory(3L, "Laptops", root, false);

        List<Category> allCategories = List.of(root, child1, child2);
        when(categoryRepository.findAll()).thenReturn(allCategories);

        CategoryResponse response = createResponse(1L, "Electronics");
        when(categoryMapper.toCategoryResponse(root)).thenReturn(response);

        // When
        List<CategoryResponse> result = categoryService.getRootCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        verify(categoryMapper, times(1)).toCategoryResponse(any(Category.class));
    }

    private Category createCategory(Long id, String name, Category parent, boolean deleted) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        category.setSlug(name.toLowerCase());
        category.setParent(parent);
        category.setHasDeleted(deleted);
        return category;
    }

    private CategoryResponse createResponse(Long id, String name) {
        return CategoryResponse.builder()
                .categoryId(id)
                .name(name)
                .slug(name.toLowerCase())
                .build();
    }
}
