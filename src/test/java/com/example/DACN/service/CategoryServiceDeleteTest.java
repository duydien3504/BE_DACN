package com.example.DACN.service;

import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.entity.Product;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.CategoryMapper;
import com.example.DACN.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Delete Category Tests")
class CategoryServiceDeleteTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category existingCategory;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        existingCategory = new Category();
        existingCategory.setCategoryId(1L);
        existingCategory.setName("Electronics");
        existingCategory.setSlug("electronics");
        existingCategory.setHasDeleted(false);
        existingCategory.setProducts(new HashSet<>());

        categoryResponse = CategoryResponse.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .hasDeleted(true)
                .build();
    }

    @Test
    @DisplayName("Should soft delete category successfully")
    void testDeleteCategorySuccess() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);
        when(categoryMapper.toCategoryResponse(existingCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.deleteCategory(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasDeleted()).isTrue();

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(existingCategory);
        verify(categoryMapper).toCategoryResponse(existingCategory);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void testDeleteCategoryNotFound() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository).findById(999L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category already deleted")
    void testDeleteCategoryAlreadyDeleted() {
        // Given
        existingCategory.setHasDeleted(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category has already been deleted");

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when category has products")
    void testDeleteCategoryWithProducts() {
        // Given
        Product product = new Product();
        product.setProductId(1L);
        Set<Product> products = new HashSet<>();
        products.add(product);
        existingCategory.setProducts(products);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete category with existing products");

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify hasDeleted is set to true")
    void testDeleteCategoryVerifyHasDeleted() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            assertThat(cat.getHasDeleted()).isTrue();
            return cat;
        });
        when(categoryMapper.toCategoryResponse(any())).thenReturn(categoryResponse);

        // When
        categoryService.deleteCategory(1L);

        // Then
        verify(categoryRepository).save(argThat(category -> category.getHasDeleted()));
    }
}
