package com.example.DACN.service;

import com.example.DACN.dto.response.CategoryTreeResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.mapper.CategoryMapper;
import com.example.DACN.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Get All Categories Tests")
class CategoryServiceGetAllTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void testGetAllCategoriesTreeEmpty() {
        // Given
        when(categoryRepository.findByParentIsNullAndHasDeletedFalse()).thenReturn(new ArrayList<>());

        // When
        List<CategoryTreeResponse> result = categoryService.getAllCategoriesTree();

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findByParentIsNullAndHasDeletedFalse();
    }

    @Test
    @DisplayName("Should return tree structure with parent-child relationships")
    void testGetAllCategoriesWithTree() {
        // Given
        Category parent = createCategory(1L, "Electronics", "electronics", null, false);
        Category child1 = createCategory(2L, "Phones", "phones", parent, false);
        Category child2 = createCategory(3L, "Laptops", "laptops", parent, false);

        parent.setSubCategories(Set.of(child1, child2));
        child1.setSubCategories(new HashSet<>());
        child2.setSubCategories(new HashSet<>());

        List<Category> rootCategories = List.of(parent);
        when(categoryRepository.findByParentIsNullAndHasDeletedFalse()).thenReturn(rootCategories);

        CategoryTreeResponse parentResponse = createTreeResponse(1L, "Electronics", List.of());
        CategoryTreeResponse child1Response = createTreeResponse(2L, "Phones", null);
        CategoryTreeResponse child2Response = createTreeResponse(3L, "Laptops", null);

        when(categoryMapper.toCategoryTreeResponse(parent)).thenReturn(parentResponse);
        when(categoryMapper.toCategoryTreeResponse(child1)).thenReturn(child1Response);
        when(categoryMapper.toCategoryTreeResponse(child2)).thenReturn(child2Response);

        // When
        List<CategoryTreeResponse> result = categoryService.getAllCategoriesTree();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        assertThat(result.get(0).getChildren()).hasSize(2);
    }

    @Test
    @DisplayName("Should exclude deleted categories from tree")
    void testGetAllCategoriesExcludeDeleted() {
        // Given
        Category parent = createCategory(1L, "Electronics", "electronics", null, false);
        Category deletedChild = createCategory(2L, "Old Category", "old", parent, true);
        Category activeChild = createCategory(3L, "Phones", "phones", parent, false);

        parent.setSubCategories(Set.of(deletedChild, activeChild));
        deletedChild.setSubCategories(new HashSet<>());
        activeChild.setSubCategories(new HashSet<>());

        List<Category> rootCategories = List.of(parent);
        when(categoryRepository.findByParentIsNullAndHasDeletedFalse()).thenReturn(rootCategories);

        CategoryTreeResponse parentResponse = createTreeResponse(1L, "Electronics", List.of());
        CategoryTreeResponse activeChildResponse = createTreeResponse(3L, "Phones", null);

        when(categoryMapper.toCategoryTreeResponse(parent)).thenReturn(parentResponse);
        when(categoryMapper.toCategoryTreeResponse(activeChild)).thenReturn(activeChildResponse);

        // When
        List<CategoryTreeResponse> result = categoryService.getAllCategoriesTree();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getCategoryId()).isEqualTo(3L);
    }

    private Category createCategory(Long id, String name, String slug, Category parent, boolean deleted) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setParent(parent);
        category.setHasDeleted(deleted);
        category.setSubCategories(new HashSet<>());
        return category;
    }

    private CategoryTreeResponse createTreeResponse(Long id, String name, List<CategoryTreeResponse> children) {
        return CategoryTreeResponse.builder()
                .categoryId(id)
                .name(name)
                .children(children)
                .build();
    }
}
