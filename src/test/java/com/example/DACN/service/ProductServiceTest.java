package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.request.CreateProductRequest;
import com.example.DACN.dto.response.CreateProductResponse;
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
@DisplayName("ProductService - Create Product Tests")
class ProductServiceTest {

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
    private CreateProductRequest request;
    private Product savedProduct;
    private CreateProductResponse response;

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

        request = new CreateProductRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("99.99"));
        request.setCategoryId(1L);
        request.setStockQuantity(10);

        savedProduct = new Product();
        savedProduct.setProductId(1L);
        savedProduct.setName("Test Product");
        savedProduct.setPrice(new BigDecimal("99.99"));

        response = CreateProductResponse.builder()
                .productId(1L)
                .message("Product created successfully")
                .build();
    }

    // Validation Tests

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void testCreateProductUserNotFound() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail("seller@example.com");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not a seller")
    void testCreateProductUserNotSeller() throws IOException {
        // Given
        Role customerRole = new Role();
        customerRole.setRoleName(RoleConstants.CUSTOMER);
        sellerUser.setRole(customerRole);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only sellers can create products");

        verify(shopRepository, never()).findByUserUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when seller has no shop")
    void testCreateProductSellerHasNoShop() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Seller does not have a shop");

        verify(shopRepository).findByUserUserId(sellerUser.getUserId());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when shop is not approved")
    void testCreateProductShopNotApproved() throws IOException {
        // Given
        approvedShop.setIsApproved(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Shop must be approved before creating products");

        verify(categoryRepository, never()).findById(any(Long.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void testCreateProductCategoryNotFound() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when category is deleted")
    void testCreateProductCategoryDeleted() throws IOException {
        // Given
        category.setHasDeleted(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create product with deleted category");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when more than 9 images")
    void testCreateProductTooManyImages() throws IOException {
        // Given
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            images.add(mock(MultipartFile.class));
        }

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct("seller@example.com", request, images))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum 9 images allowed");

        verify(cloudinaryService, never()).uploadProductImages(any());
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should create product successfully without images")
    void testCreateProductSuccessWithoutImages() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        CreateProductResponse result = productService.createProduct("seller@example.com", request, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("Product created successfully");

        verify(productRepository).save(any(Product.class));
        verify(cloudinaryService, never()).uploadProductImages(any());
        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("Should create product successfully with images")
    void testCreateProductSuccessWithImages() throws IOException {
        // Given
        List<MultipartFile> images = Arrays.asList(
                mock(MultipartFile.class),
                mock(MultipartFile.class),
                mock(MultipartFile.class));

        List<String> imageUrls = Arrays.asList(
                "https://cloudinary.com/image1.jpg",
                "https://cloudinary.com/image2.jpg",
                "https://cloudinary.com/image3.jpg");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(cloudinaryService.uploadProductImages(images)).thenReturn(imageUrls);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        CreateProductResponse result = productService.createProduct("seller@example.com", request, images);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);

        verify(cloudinaryService).uploadProductImages(images);
        verify(productRepository).save(any(Product.class));
        verify(productImageRepository, times(3)).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("Should create product with correct properties")
    void testProductCreatedWithCorrectProperties() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.createProduct("seller@example.com", request, null);

        // Then
        verify(productRepository).save(argThat(product -> product.getName().equals("Test Product") &&
                product.getDescription().equals("Test Description") &&
                product.getPrice().equals(new BigDecimal("99.99")) &&
                product.getStockQuantity().equals(10) &&
                product.getSoldCount().equals(0) &&
                product.getStatus().equals("Active") &&
                !product.getHasDeleted()));
    }

    @Test
    @DisplayName("Should create product images with correct display order")
    void testProductImagesCreatedWithCorrectOrder() throws IOException {
        // Given
        List<MultipartFile> images = Arrays.asList(
                mock(MultipartFile.class),
                mock(MultipartFile.class));

        List<String> imageUrls = Arrays.asList(
                "https://cloudinary.com/image1.jpg",
                "https://cloudinary.com/image2.jpg");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(cloudinaryService.uploadProductImages(images)).thenReturn(imageUrls);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.createProduct("seller@example.com", request, images);

        // Then
        verify(productImageRepository)
                .save(argThat(productImage -> productImage.getImageUrl().equals("https://cloudinary.com/image1.jpg") &&
                        productImage.getDisplayOrder().equals(0)));

        verify(productImageRepository)
                .save(argThat(productImage -> productImage.getImageUrl().equals("https://cloudinary.com/image2.jpg") &&
                        productImage.getDisplayOrder().equals(1)));
    }

    @Test
    @DisplayName("Should set default stock quantity when null")
    void testDefaultStockQuantity() throws IOException {
        // Given
        request.setStockQuantity(null);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.createProduct("seller@example.com", request, null);

        // Then
        verify(productRepository).save(argThat(product -> product.getStockQuantity().equals(0)));
    }

    @Test
    @DisplayName("Should handle empty image list")
    void testCreateProductWithEmptyImageList() throws IOException {
        // Given
        List<MultipartFile> emptyImages = new ArrayList<>();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        CreateProductResponse result = productService.createProduct("seller@example.com", request, emptyImages);

        // Then
        assertThat(result).isNotNull();
        verify(cloudinaryService, never()).uploadProductImages(any());
        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("Should verify all dependencies are called in correct order")
    void testDependenciesCalledInOrder() throws IOException {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toCreateProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.createProduct("seller@example.com", request, null);

        // Then
        var inOrder = inOrder(userRepository, shopRepository, categoryRepository, productRepository, productMapper);
        inOrder.verify(userRepository).findByEmail("seller@example.com");
        inOrder.verify(shopRepository).findByUserUserId(sellerUser.getUserId());
        inOrder.verify(categoryRepository).findById(1L);
        inOrder.verify(productRepository).save(any(Product.class));
        inOrder.verify(productMapper).toCreateProductResponse(savedProduct);
    }
}
