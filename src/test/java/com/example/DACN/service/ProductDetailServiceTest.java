package com.example.DACN.service;

import com.example.DACN.dto.response.ProductDetailResponse;
import com.example.DACN.entity.Category;
import com.example.DACN.entity.Product;
import com.example.DACN.entity.ProductImage;
import com.example.DACN.entity.Shop;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.ProductMapper;
import com.example.DACN.repository.ProductImageRepository;
import com.example.DACN.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Get Product By ID Tests")
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Shop shop;
    private Category category;
    private ProductImage image1;
    private ProductImage image2;
    private ProductDetailResponse productDetailResponse;

    @BeforeEach
    void setUp() {
        shop = new Shop();
        shop.setShopId(1L);
        shop.setShopName("Test Shop");
        shop.setShopDescription("Test Shop Description");

        category = new Category();
        category.setCategoryId(1L);
        category.setName("Test Category");

        product = new Product();
        product.setProductId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setStockQuantity(10);
        product.setSoldCount(5);
        product.setStatus("Active");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setShop(shop);
        product.setCategory(category);
        product.setHasDeleted(false);

        image1 = new ProductImage();
        image1.setProductImageId(1L);
        image1.setImageUrl("https://example.com/image1.jpg");
        image1.setDisplayOrder(0);

        image2 = new ProductImage();
        image2.setProductImageId(2L);
        image2.setImageUrl("https://example.com/image2.jpg");
        image2.setDisplayOrder(1);

        productDetailResponse = ProductDetailResponse.builder()
                .productId(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .soldCount(5)
                .status("Active")
                .shopId(1L)
                .shopName("Test Shop")
                .shopDescription("Test Shop Description")
                .categoryId(1L)
                .categoryName("Test Category")
                .build();
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should return product details successfully")
    void testGetProductByIdSuccess() {
        // Given
        List<ProductImage> images = Arrays.asList(image1, image2);

        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong()))
                .thenReturn(Optional.of(product));
        when(productMapper.toProductDetailResponse(any(Product.class)))
                .thenReturn(productDetailResponse);
        when(productImageRepository.findByProductProductIdOrderByDisplayOrderAsc(anyLong()))
                .thenReturn(images);

        // When
        ProductDetailResponse result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getImages()).hasSize(2);
        assertThat(result.getImages().get(0)).isEqualTo("https://example.com/image1.jpg");
        assertThat(result.getImages().get(1)).isEqualTo("https://example.com/image2.jpg");

        verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        verify(productMapper).toProductDetailResponse(product);
        verify(productImageRepository).findByProductProductIdOrderByDisplayOrderAsc(1L);
    }

    @Test
    @DisplayName("Should return product with empty images list")
    void testGetProductByIdWithNoImages() {
        // Given
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong()))
                .thenReturn(Optional.of(product));
        when(productMapper.toProductDetailResponse(any(Product.class)))
                .thenReturn(productDetailResponse);
        when(productImageRepository.findByProductProductIdOrderByDisplayOrderAsc(anyLong()))
                .thenReturn(Collections.emptyList());

        // When
        ProductDetailResponse result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImages()).isEmpty();
    }

    // Error Handling Tests

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void testGetProductByIdNotFound() {
        // Given
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verify(productRepository).findByProductIdAndHasDeletedFalse(999L);
        verify(productMapper, never()).toProductDetailResponse(any(Product.class));
        verify(productImageRepository, never()).findByProductProductIdOrderByDisplayOrderAsc(anyLong());
    }

    @Test
    @DisplayName("Should not return deleted products")
    void testGetProductByIdDeletedProduct() {
        // Given
        product.setHasDeleted(true);
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    @Test
    @DisplayName("Should verify all dependencies are called in correct order")
    void testGetProductByIdDependenciesCalledInOrder() {
        // Given
        List<ProductImage> images = Arrays.asList(image1);

        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong()))
                .thenReturn(Optional.of(product));
        when(productMapper.toProductDetailResponse(any(Product.class)))
                .thenReturn(productDetailResponse);
        when(productImageRepository.findByProductProductIdOrderByDisplayOrderAsc(anyLong()))
                .thenReturn(images);

        // When
        productService.getProductById(1L);

        // Then
        var inOrder = inOrder(productRepository, productMapper, productImageRepository);
        inOrder.verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        inOrder.verify(productMapper).toProductDetailResponse(product);
        inOrder.verify(productImageRepository).findByProductProductIdOrderByDisplayOrderAsc(1L);
    }
}
