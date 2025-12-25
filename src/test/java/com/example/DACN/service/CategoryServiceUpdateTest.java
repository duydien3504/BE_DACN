package com.example.DACN.service;

import com.example.DACN.dto.request.UpdateCategoryRequest;
import com.example.DACN.dto.response.CategoryResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.exception.DuplicateResourceException;
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
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Update Category Tests")
class CategoryServiceUpdateTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category existingCategory;
    private UpdateCategoryRequest request;
    private CategoryResponse categoryResponse;
    private MockMultipartFile iconFile;

    @BeforeEach
    void setUp() {
        existingCategory = new Category();
        existingCategory.setCategoryId(1L);
        existingCategory.setName("Old Name");
        existingCategory.setSlug("old-slug");
        existingCategory.setIconUrl("https://cloudinary.com/old-icon.png");
        existingCategory.setHasDeleted(false);

        request = UpdateCategoryRequest.builder()
                .name("Updated Name")
                .slug("updated-slug")
                .build();

        categoryResponse = CategoryResponse.builder()
                .categoryId(1L)
                .name("Updated Name")
                .slug("updated-slug")
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
    @DisplayName("Should update all fields successfully")
    void testUpdateCategoryAllFieldsSuccess() throws IOException {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsBySlug("updated-slug")).thenReturn(false);
        when(cloudinaryService.uploadImage(iconFile)).thenReturn("https://cloudinary.com/new-icon.png");
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);
        when(categoryMapper.toCategoryResponse(existingCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.updateCategory(1L, request, iconFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getSlug()).isEqualTo("updated-slug");

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).existsBySlug("updated-slug");
        verify(cloudinaryService).deleteImage("https://cloudinary.com/old-icon.png");
        verify(cloudinaryService).uploadImage(iconFile);
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    @DisplayName("Should update only name")
    void testUpdateCategoryNameOnly() throws IOException {
        // Given
        request.setSlug(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);
        when(categoryMapper.toCategoryResponse(existingCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.updateCategory(1L, request, null);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).existsBySlug(any());
        verify(cloudinaryService, never()).uploadImage(any());
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    @DisplayName("Should update with new icon and delete old icon")
    void testUpdateCategoryWithNewIcon() throws IOException {
        // Given
        request.setName(null);
        request.setSlug(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(cloudinaryService.uploadImage(iconFile)).thenReturn("https://cloudinary.com/new-icon.png");
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);
        when(categoryMapper.toCategoryResponse(existingCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.updateCategory(1L, request, iconFile);

        // Then
        assertThat(result).isNotNull();
        verify(cloudinaryService).deleteImage("https://cloudinary.com/old-icon.png");
        verify(cloudinaryService).uploadImage(iconFile);
    }

    @Test
    @DisplayName("Should update parent category")
    void testUpdateCategoryParent() throws IOException {
        // Given
        request.setName(null);
        request.setSlug(null);
        request.setParentId(10L);

        Category parentCategory = new Category();
        parentCategory.setCategoryId(10L);
        parentCategory.setName("Parent");
        parentCategory.setHasDeleted(false);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);
        when(categoryMapper.toCategoryResponse(existingCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.updateCategory(1L, request, null);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository).findById(10L);
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void testUpdateCategoryNotFound() {
        // Given
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(999L, request, iconFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository).findById(999L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category is deleted")
    void testUpdateCategoryDeleted() {
        // Given
        existingCategory.setHasDeleted(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, request, iconFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category has been deleted");

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when new slug already exists")
    void testUpdateCategoryDuplicateSlug() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsBySlug("updated-slug")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, request, iconFile))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category with slug 'updated-slug' already exists");

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).existsBySlug("updated-slug");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent category not found")
    void testUpdateCategoryParentNotFound() {
        // Given
        request.setParentId(999L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsBySlug("updated-slug")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, request, iconFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Parent category not found");

        verify(categoryRepository).findById(999L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when category is its own parent")
    void testUpdateCategoryCircularRelationship() {
        // Given
        request.setParentId(1L);
        Category selfCategory = new Category();
        selfCategory.setCategoryId(1L);
        selfCategory.setHasDeleted(false);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsBySlug("updated-slug")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(selfCategory));

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, request, iconFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category cannot be its own parent");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not check slug uniqueness if slug unchanged")
    void testUpdateCategorySlugUnchanged() throws IOException {
        // Given
        request.setSlug("old-slug"); // Same as existing
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);
        when(categoryMapper.toCategoryResponse(existingCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.updateCategory(1L, request, null);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository, never()).existsBySlug(any());
    }
}
