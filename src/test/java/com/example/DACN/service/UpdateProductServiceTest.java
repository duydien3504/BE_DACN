package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.UpdateProductRequest;
import com.example.DACN.dto.response.UpdateProductResponse;
import com.example.DACN.entity.*;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.exception.UnauthorizedException;
import com.example.DACN.mapper.ProductMapper;
import com.example.DACN.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Update Product Tests")
class UpdateProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private User sellerUser;
    private Role sellerRole;
    private Shop approvedShop;
    private Category category;
    private Product existingProduct;
    private UpdateProductRequest request;
    private UpdateProductResponse response;

    @BeforeEach
    void setUp() {
        sellerRole = new Role();
        sellerRole.setRoleId(3L);
        sellerRole.setRoleName(RoleConstants.SELLER);

        sellerUser = new User();
        sellerUser.setUserId(UUID.randomUUID());
        sellerUser.setEmail("seller@example.com");
        sellerUser.setFullName("Test Seller");
        sellerUser.setRole(sellerRole);

        approvedShop = new Shop();
        approvedShop.setShopId(1L);
        approvedShop.setShopName("Test Shop");
        approvedShop.setIsApproved(true);
        approvedShop.setHasDeleted(false);
        approvedShop.setUser(sellerUser);

        category = new Category();
        category.setCategoryId(1L);
        category.setName("Electronics");
        category.setHasDeleted(false);

        existingProduct = new Product();
        existingProduct.setProductId(1L);
        existingProduct.setShop(approvedShop);
        existingProduct.setCategory(category);
        existingProduct.setName("Original Product");
        existingProduct.setDescription("Original Description");
        existingProduct.setPrice(new BigDecimal("99.99"));
        existingProduct.setStockQuantity(10);
        existingProduct.setStatus("Active");
        existingProduct.setHasDeleted(false);

        request = new UpdateProductRequest();
        request.setName("Updated Product");
        request.setDescription("Updated Description");
        request.setPrice(new BigDecimal("149.99"));
        request.setCategoryId(2L);
        request.setStockQuantity(20);
        request.setStatus("Inactive");

        response = UpdateProductResponse.builder()
                .productId(1L)
                .message("Product updated successfully")
                .build();
    }

    // Validation Tests

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void testUpdateProductUserNotFound() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail("seller@example.com");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not a seller")
    void testUpdateProductUserNotSeller() throws IOException {
        // Given
        Role customerRole = new Role();
        customerRole.setRoleName(RoleConstants.CUSTOMER);
        sellerUser.setRole(customerRole);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, request, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only sellers can update products");

        verify(shopRepository, never()).findByUserUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when seller has no shop")
    void testUpdateProductSellerHasNoShop() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Seller does not have a shop");

        verify(shopRepository).findByUserUserId(sellerUser.getUserId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void testUpdateProductNotFound() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 999L, request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verify(productRepository).findByProductIdAndHasDeletedFalse(999L);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when product doesn't belong to seller")
    void testUpdateProductNotOwner() throws IOException {
        // Given
        Shop anotherShop = new Shop();
        anotherShop.setShopId(2L);
        existingProduct.setShop(anotherShop);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, request, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only update your own products");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void testUpdateProductCategoryNotFound() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository).findById(2L);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when category is deleted")
    void testUpdateProductCategoryDeleted() throws IOException {
        // Given
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(2L);
        deletedCategory.setHasDeleted(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(deletedCategory));

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, request, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot update product with deleted category");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when more than 9 images")
    void testUpdateProductTooManyImages() throws IOException {
        // Given
        UpdateProductRequest simpleRequest = new UpdateProductRequest();
        simpleRequest.setName("Updated Name");

        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            images.add(mock(MultipartFile.class));
        }

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct("seller@example.com", 1L, simpleRequest, images))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum 9 images allowed");

        verify(cloudinaryService, never()).uploadProductImages(any());
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should update product successfully without images")
    void testUpdateProductSuccessWithoutImages() throws IOException {
        // Given
        Category newCategory = new Category();
        newCategory.setCategoryId(2L);
        newCategory.setHasDeleted(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toUpdateProductResponse(any(Product.class))).thenReturn(response);

        // When
        UpdateProductResponse result = productService.updateProduct("seller@example.com", 1L, request, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("Product updated successfully");

        verify(productRepository).save(any(Product.class));
        verify(cloudinaryService, never()).uploadProductImages(any());
        verify(productImageRepository, never()).deleteByProductProductId(anyLong());
    }

    @Test
    @DisplayName("Should update product successfully with new images")
    void testUpdateProductSuccessWithNewImages() throws IOException {
        // Given
        List<MultipartFile> images = Arrays.asList(
                mock(MultipartFile.class),
                mock(MultipartFile.class));

        List<String> imageUrls = Arrays.asList(
                "https://cloudinary.com/new1.jpg",
                "https://cloudinary.com/new2.jpg");

        ProductImage oldImage1 = new ProductImage();
        oldImage1.setImageUrl("https://cloudinary.com/old1.jpg");
        ProductImage oldImage2 = new ProductImage();
        oldImage2.setImageUrl("https://cloudinary.com/old2.jpg");
        List<ProductImage> existingImages = Arrays.asList(oldImage1, oldImage2);

        Category newCategory = new Category();
        newCategory.setCategoryId(2L);
        newCategory.setHasDeleted(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(newCategory));
        when(productImageRepository.findByProductProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(existingImages);
        when(cloudinaryService.uploadProductImages(images)).thenReturn(imageUrls);
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toUpdateProductResponse(any(Product.class))).thenReturn(response);

        // When
        UpdateProductResponse result = productService.updateProduct("seller@example.com", 1L, request, images);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);

        verify(cloudinaryService, times(2)).deleteImage(anyString());
        verify(productImageRepository).deleteByProductProductId(1L);
        verify(cloudinaryService).uploadProductImages(images);
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product with partial fields")
    void testUpdateProductPartialUpdate() throws IOException {
        // Given
        UpdateProductRequest partialRequest = new UpdateProductRequest();
        partialRequest.setName("New Name Only");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toUpdateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.updateProduct("seller@example.com", 1L, partialRequest, null);

        // Then
        verify(productRepository).save(argThat(product -> product.getName().equals("New Name Only") &&
                product.getDescription().equals("Original Description") &&
                product.getPrice().equals(new BigDecimal("99.99"))));
    }

    @Test
    @DisplayName("Should delete old images when new images provided")
    void testOldImagesDeletedWhenNewImagesProvided() throws IOException {
        // Given
        UpdateProductRequest simpleRequest = new UpdateProductRequest();
        simpleRequest.setName("Updated Name");

        List<MultipartFile> images = Arrays.asList(
                mock(MultipartFile.class));
        List<String> imageUrls = Arrays.asList("https://cloudinary.com/new.jpg");

        ProductImage oldImage = new ProductImage();
        oldImage.setImageUrl("https://cloudinary.com/old.jpg");
        List<ProductImage> existingImages = Arrays.asList(oldImage);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(productImageRepository.findByProductProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(existingImages);
        when(cloudinaryService.uploadProductImages(images)).thenReturn(imageUrls);
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toUpdateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.updateProduct("seller@example.com", 1L, simpleRequest, images);

        // Then
        verify(cloudinaryService).deleteImage("https://cloudinary.com/old.jpg");
        verify(productImageRepository).deleteByProductProductId(1L);
    }

    @Test
    @DisplayName("Should update product with correct properties")
    void testProductUpdatedWithCorrectProperties() throws IOException {
        // Given
        Category newCategory = new Category();
        newCategory.setCategoryId(2L);
        newCategory.setHasDeleted(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(newCategory));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toUpdateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.updateProduct("seller@example.com", 1L, request, null);

        // Then
        verify(productRepository).save(argThat(product -> product.getName().equals("Updated Product") &&
                product.getDescription().equals("Updated Description") &&
                product.getPrice().equals(new BigDecimal("149.99")) &&
                product.getStockQuantity().equals(20) &&
                product.getStatus().equals("Inactive")));
    }

    @Test
    @DisplayName("Should verify all dependencies are called in correct order")
    void testDependenciesCalledInOrder() throws IOException {
        // Given
        UpdateProductRequest simpleRequest = new UpdateProductRequest();
        simpleRequest.setName("Updated Name");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toUpdateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.updateProduct("seller@example.com", 1L, simpleRequest, null);

        // Then
        var inOrder = inOrder(userRepository, shopRepository, productRepository, productMapper);
        inOrder.verify(userRepository).findByEmail("seller@example.com");
        inOrder.verify(shopRepository).findByUserUserId(sellerUser.getUserId());
        inOrder.verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        inOrder.verify(productRepository).save(any(Product.class));
        inOrder.verify(productMapper).toUpdateProductResponse(existingProduct);
    }
}
