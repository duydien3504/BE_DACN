package com.example.DACN.service;

import com.example.DACN.constant.RoleConstants;
import com.example.DACN.dto.response.DeleteProductResponse;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Delete Product Tests")
class DeleteProductServiceTest {

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
    private DeleteProductResponse response;

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
        existingProduct.setName("Test Product");
        existingProduct.setDescription("Test Description");
        existingProduct.setPrice(new BigDecimal("99.99"));
        existingProduct.setStockQuantity(10);
        existingProduct.setStatus("Active");
        existingProduct.setHasDeleted(false);

        response = DeleteProductResponse.builder()
                .productId(1L)
                .message("Product deleted successfully")
                .build();
    }

    // Validation Tests

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void testDeleteProductUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct("seller@example.com", 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository).findByEmail("seller@example.com");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not a seller")
    void testDeleteProductUserNotSeller() {
        // Given
        Role customerRole = new Role();
        customerRole.setRoleName(RoleConstants.CUSTOMER);
        sellerUser.setRole(customerRole);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct("seller@example.com", 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only sellers can delete products");

        verify(shopRepository, never()).findByUserUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when seller has no shop")
    void testDeleteProductSellerHasNoShop() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct("seller@example.com", 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Seller does not have a shop");

        verify(shopRepository).findByUserUserId(sellerUser.getUserId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void testDeleteProductNotFound() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct("seller@example.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verify(productRepository).findByProductIdAndHasDeletedFalse(999L);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when product doesn't belong to seller")
    void testDeleteProductNotOwner() {
        // Given
        Shop anotherShop = new Shop();
        anotherShop.setShopId(2L);
        existingProduct.setShop(anotherShop);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct("seller@example.com", 1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only delete your own products");

        verify(productRepository, never()).save(any(Product.class));
    }

    // Core Logic Tests

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProductSuccess() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toDeleteProductResponse(any(Product.class))).thenReturn(response);

        // When
        DeleteProductResponse result = productService.deleteProduct("seller@example.com", 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("Product deleted successfully");

        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should set hasDeleted to true")
    void testProductMarkedAsDeleted() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toDeleteProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.deleteProduct("seller@example.com", 1L);

        // Then
        verify(productRepository).save(argThat(product -> product.getHasDeleted()));
    }

    @Test
    @DisplayName("Should verify all dependencies are called in correct order")
    void testDependenciesCalledInOrder() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sellerUser));
        when(shopRepository.findByUserUserId(any(UUID.class))).thenReturn(Optional.of(approvedShop));
        when(productRepository.findByProductIdAndHasDeletedFalse(anyLong())).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);
        when(productMapper.toDeleteProductResponse(any(Product.class))).thenReturn(response);

        // When
        productService.deleteProduct("seller@example.com", 1L);

        // Then
        var inOrder = inOrder(userRepository, shopRepository, productRepository, productMapper);
        inOrder.verify(userRepository).findByEmail("seller@example.com");
        inOrder.verify(shopRepository).findByUserUserId(sellerUser.getUserId());
        inOrder.verify(productRepository).findByProductIdAndHasDeletedFalse(1L);
        inOrder.verify(productRepository).save(any(Product.class));
        inOrder.verify(productMapper).toDeleteProductResponse(existingProduct);
    }
}
