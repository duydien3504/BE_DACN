package com.example.DACN.service;

import com.example.DACN.dto.request.AddCartItemRequest;
import com.example.DACN.dto.response.AddCartItemResponse;
import com.example.DACN.entity.Cart;
import com.example.DACN.entity.CartItem;
import com.example.DACN.entity.Product;
import com.example.DACN.entity.User;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.CartMapper;
import com.example.DACN.repository.CartItemRepository;
import com.example.DACN.repository.CartRepository;
import com.example.DACN.repository.ProductRepository;
import com.example.DACN.repository.UserRepository;
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
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private AddCartItemRequest request;
    private AddCartItemResponse response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("user@example.com");

        product = new Product();
        product.setProductId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus("Active");
        product.setStockQuantity(10);
        product.setHasDeleted(false);

        cart = new Cart();
        cart.setCartId(1L);
        cart.setUser(user);

        cartItem = new CartItem();
        cartItem.setCartItemId(1L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        request = new AddCartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        response = new AddCartItemResponse();
        response.setCartItemId(1L);
        response.setQuantity(2); // Using the real DTO field
    }

    @Test
    @DisplayName("Should create new cart item when product not in cart")
    void addCartItem_NewItem() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserUserId(user.getUserId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndProductProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setCartItemId(1L);
            return item;
        });
        when(cartMapper.toAddCartItemResponse(any(CartItem.class))).thenReturn(response);

        // When
        AddCartItemResponse result = cartService.addCartItem("user@example.com", request);

        // Then
        assertThat(result).isNotNull();
        verify(cartItemRepository).save(argThat(item -> item.getProduct().equals(product) &&
                item.getQuantity().equals(2)));
    }

    @Test
    @DisplayName("Should update quantity when product already in cart")
    void addCartItem_UpdateQuantity() {
        // Given
        CartItem existingItem = new CartItem();
        existingItem.setCartItemId(1L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(3);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserUserId(user.getUserId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndProductProductId(1L, 1L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existingItem);
        when(cartMapper.toAddCartItemResponse(any(CartItem.class))).thenReturn(response);

        // When
        cartService.addCartItem("user@example.com", request);

        // Then (Expected quantity = 3 + 2 = 5)
        verify(cartItemRepository).save(argThat(item -> item.getCartItemId().equals(1L) &&
                item.getQuantity().equals(5)));
    }

    @Test
    @DisplayName("Should create new cart if user doesn't have one")
    void addCartItem_CreateNewCart() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserUserId(user.getUserId())).thenReturn(Optional.empty()); // No cart
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart c = invocation.getArgument(0);
            c.setCartId(1L);
            return c;
        });
        when(cartItemRepository.findByCartCartIdAndProductProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartMapper.toAddCartItemResponse(any(CartItem.class))).thenReturn(response);

        // When
        cartService.addCartItem("user@example.com", request);

        // Then
        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception if stock insufficient")
    void addCartItem_InsufficientStock() {
        // Given
        product.setStockQuantity(1); // Stock less than request (2)
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product));

        // When & Then
        assertThatThrownBy(() -> cartService.addCartItem("user@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should throw exception if product not active")
    void addCartItem_ProductNotActive() {
        // Given
        product.setStatus("Inactive");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(productRepository.findByProductIdAndHasDeletedFalse(1L)).thenReturn(Optional.of(product));

        // When & Then
        assertThatThrownBy(() -> cartService.addCartItem("user@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Product is not active");
    }

    @Test
    @DisplayName("Should get cart successfully with items")
    void getCart_Success_WithItems() {
        // Given
        java.util.Set<CartItem> items = new java.util.HashSet<>();
        items.add(cartItem);
        cart.setCartItems(items);

        com.example.DACN.dto.response.CartItemResponse itemResponse = com.example.DACN.dto.response.CartItemResponse
                .builder()
                .cartItemId(1L)
                .productId(1L)
                .quantity(2)
                .subtotal(new BigDecimal("200.00"))
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartRepository.findByUserUserId(user.getUserId())).thenReturn(Optional.of(cart));
        when(cartMapper.toCartItemResponse(any(CartItem.class))).thenReturn(itemResponse);

        // When
        com.example.DACN.dto.response.CartResponse result = cartService.getCart("user@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCartId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalPrice()).isEqualTo(new BigDecimal("200.00"));

        verify(cartRepository).findByUserUserId(user.getUserId());
    }

    @Test
    @DisplayName("Should get empty cart when no items")
    void getCart_Success_Empty() {
        // Given
        cart.setCartItems(new java.util.HashSet<>());

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartRepository.findByUserUserId(user.getUserId())).thenReturn(Optional.of(cart));

        // When
        com.example.DACN.dto.response.CartResponse result = cartService.getCart("user@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should update cart item successfully")
    void updateCartItem_Success() {
        // Given
        com.example.DACN.dto.request.UpdateCartItemRequest updateRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                .builder()
                .quantity(5)
                .build();

        com.example.DACN.dto.response.CartItemResponse cartItemResponse = com.example.DACN.dto.response.CartItemResponse
                .builder()
                .cartItemId(1L)
                .quantity(5)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartMapper.toCartItemResponse(any(CartItem.class))).thenReturn(cartItemResponse);

        // When
        com.example.DACN.dto.response.CartItemResponse result = cartService.updateCartItem("user@example.com", 1L,
                updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when updating item not in user's cart")
    void updateCartItem_NotOwned() {
        // Given
        User otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        Cart otherCart = new Cart();
        otherCart.setUser(otherUser);
        CartItem otherItem = new CartItem();
        otherItem.setCart(otherCart); // Item belongs to other user

        com.example.DACN.dto.request.UpdateCartItemRequest updateRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                .builder()
                .quantity(5)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(otherItem));

        // When & Then
        assertThatThrownBy(() -> cartService.updateCartItem("user@example.com", 1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found in user's cart");
    }

    @Test
    @DisplayName("Should throw exception when updating item with insufficient stock")
    void updateCartItem_InsufficientStock() {
        // Given
        product.setStockQuantity(3);
        com.example.DACN.dto.request.UpdateCartItemRequest updateRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                .builder()
                .quantity(5) // > stock
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));

        // When & Then
        assertThatThrownBy(() -> cartService.updateCartItem("user@example.com", 1L, updateRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should throw exception when cart item not found")
    void updateCartItem_NotFound() {
        // Given
        com.example.DACN.dto.request.UpdateCartItemRequest updateRequest = com.example.DACN.dto.request.UpdateCartItemRequest
                .builder()
                .quantity(5)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartService.updateCartItem("user@example.com", 1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart item not found");
    }
}
