package com.example.DACN.service;

import com.example.DACN.dto.request.CreateCategoryRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Create Category Tests")
class CategoryServiceCreateTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private CreateCategoryRequest request;
    private Category savedCategory;
    private CategoryResponse categoryResponse;
    private MockMultipartFile iconFile;

    @BeforeEach
    void setUp() {
        request = CreateCategoryRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .parentId(null)
                .build();

        savedCategory = new Category();
        savedCategory.setCategoryId(1L);
        savedCategory.setName("Electronics");
        savedCategory.setSlug("electronics");
        savedCategory.setIconUrl("https://cloudinary.com/icon.png");
        savedCategory.setHasDeleted(false);

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
    @DisplayName("Should create root category with icon successfully")
    void testCreateCategoryWithIconSuccess() throws IOException {
        // Given
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(cloudinaryService.uploadImage(iconFile)).thenReturn("https://cloudinary.com/icon.png");
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.createCategory(request, iconFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electronics");
        assertThat(result.getSlug()).isEqualTo("electronics");
        assertThat(result.getIconUrl()).isEqualTo("https://cloudinary.com/icon.png");
        assertThat(result.getParentId()).isNull();

        verify(categoryRepository).existsBySlug("electronics");
        verify(cloudinaryService).uploadImage(iconFile);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toCategoryResponse(savedCategory);
    }

    @Test
    @DisplayName("Should create category without icon successfully")
    void testCreateCategoryWithoutIcon() throws IOException {
        // Given
        savedCategory.setIconUrl(null);
        categoryResponse.setIconUrl(null);

        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.createCategory(request, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIconUrl()).isNull();

        verify(categoryRepository).existsBySlug("electronics");
        verify(cloudinaryService, never()).uploadImage(any());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should create subcategory with parent successfully")
    void testCreateSubcategorySuccess() throws IOException {
        // Given
        request.setParentId(10L);

        Category parentCategory = new Category();
        parentCategory.setCategoryId(10L);
        parentCategory.setName("Parent Category");
        parentCategory.setHasDeleted(false);

        savedCategory.setParent(parentCategory);
        categoryResponse.setParentId(10L);

        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parentCategory));
        when(cloudinaryService.uploadImage(iconFile)).thenReturn("https://cloudinary.com/icon.png");
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.createCategory(request, iconFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(10L);

        verify(categoryRepository).findById(10L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when slug already exists")
    void testCreateCategoryDuplicateSlug() throws IOException {
        // Given
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(request, iconFile))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category with slug 'electronics' already exists");

        verify(categoryRepository).existsBySlug("electronics");
        verify(categoryRepository, never()).save(any());
        verify(cloudinaryService, never()).uploadImage(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent category not found")
    void testCreateCategoryParentNotFound() throws IOException {
        // Given
        request.setParentId(999L);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(request, iconFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Parent category not found");

        verify(categoryRepository).existsBySlug("electronics");
        verify(categoryRepository).findById(999L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent category is deleted")
    void testCreateCategoryParentDeleted() throws IOException {
        // Given
        request.setParentId(10L);

        Category deletedParent = new Category();
        deletedParent.setCategoryId(10L);
        deletedParent.setHasDeleted(true);

        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(deletedParent));

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(request, iconFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Parent category has been deleted");

        verify(categoryRepository).findById(10L);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify correct response structure mapping")
    void testCreateCategoryResponseStructure() throws IOException {
        // Given
        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(cloudinaryService.uploadImage(iconFile)).thenReturn("https://cloudinary.com/icon.png");
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.createCategory(request, iconFile);

        // Then
        assertThat(result.getCategoryId()).isNotNull();
        assertThat(result.getName()).isNotNull();
        assertThat(result.getSlug()).isNotNull();
        assertThat(result.getHasDeleted()).isNotNull();

        verify(categoryMapper).toCategoryResponse(argThat(category -> category.getName().equals("Electronics") &&
                category.getSlug().equals("electronics") &&
                !category.getHasDeleted()));
    }

    @Test
    @DisplayName("Should handle empty multipart file")
    void testCreateCategoryEmptyFile() throws IOException {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "icon",
                "icon.png",
                "image/png",
                new byte[0]);

        savedCategory.setIconUrl(null);
        categoryResponse.setIconUrl(null);

        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toCategoryResponse(savedCategory)).thenReturn(categoryResponse);

        // When
        CategoryResponse result = categoryService.createCategory(request, emptyFile);

        // Then
        assertThat(result.getIconUrl()).isNull();
        verify(cloudinaryService, never()).uploadImage(any());
    }
}
