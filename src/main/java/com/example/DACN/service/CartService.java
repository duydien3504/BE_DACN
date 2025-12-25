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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final CartMapper cartMapper;

        public AddCartItemResponse addCartItem(String userEmail, AddCartItemRequest request) {
                log.info("Adding item to cart for user: {}, productId: {}, quantity: {}", userEmail,
                                request.getProductId(),
                                request.getQuantity());

                // 1. Get User
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // 2. Get Product
                Product product = productRepository.findByProductIdAndHasDeletedFalse(request.getProductId())
                                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                if (!"Active".equals(product.getStatus())) {
                        throw new IllegalStateException("Product is not active");
                }

                if (product.getStockQuantity() < request.getQuantity()) {
                        throw new IllegalStateException("Insufficient stock. Available: " + product.getStockQuantity());
                }

                // 3. Get or Create Cart
                Cart cart = cartRepository.findByUserUserId(user.getUserId())
                                .orElseGet(() -> {
                                        log.info("Creating new cart for user: {}", user.getUserId());
                                        Cart newCart = new Cart();
                                        newCart.setUser(user);
                                        return cartRepository.save(newCart);
                                });

                // 4. Check if item exists in cart
                Optional<CartItem> existingItemOpt = cartItemRepository.findByCartCartIdAndProductProductId(
                                cart.getCartId(),
                                product.getProductId());

                CartItem savedItem;
                if (existingItemOpt.isPresent()) {
                        // Update quantity
                        CartItem existingItem = existingItemOpt.get();
                        log.info("Updating existing cart item: {}, old quantity: {}", existingItem.getCartItemId(),
                                        existingItem.getQuantity());

                        int newQuantity = existingItem.getQuantity() + request.getQuantity();

                        // Check stock again for the total new quantity
                        if (product.getStockQuantity() < newQuantity) {
                                throw new IllegalStateException(
                                                "Insufficient stock for total quantity. Available: "
                                                                + product.getStockQuantity());
                        }

                        existingItem.setQuantity(newQuantity);
                        savedItem = cartItemRepository.save(existingItem);
                } else {
                        // Create new item
                        log.info("Creating new cart item");
                        CartItem newItem = new CartItem();
                        newItem.setCart(cart);
                        newItem.setProduct(product);
                        newItem.setQuantity(request.getQuantity());
                        savedItem = cartItemRepository.save(newItem);
                }

                // 5. Map to Response
                AddCartItemResponse response = cartMapper.toAddCartItemResponse(savedItem);
                response.setMessage("Item added to cart successfully");

                return response;

        }

        public com.example.DACN.dto.response.CartResponse getCart(String userEmail) {
                log.info("Getting cart for user: {}", userEmail);

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserUserId(user.getUserId())
                                .orElseGet(() -> {
                                        log.info("Creating new cart for user: {}", user.getUserId());
                                        Cart newCart = new Cart();
                                        newCart.setUser(user);
                                        return cartRepository.save(newCart);
                                });

                java.util.List<com.example.DACN.dto.response.CartItemResponse> itemResponses = new java.util.ArrayList<>();
                java.math.BigDecimal totalPrice = java.math.BigDecimal.ZERO;

                if (cart.getCartItems() != null) {
                        itemResponses = cart.getCartItems().stream()
                                        .map(cartMapper::toCartItemResponse)
                                        .collect(java.util.stream.Collectors.toList());

                        totalPrice = itemResponses.stream()
                                        .map(com.example.DACN.dto.response.CartItemResponse::getSubtotal)
                                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                }

                return com.example.DACN.dto.response.CartResponse.builder()
                                .cartId(cart.getCartId())
                                .items(itemResponses)
                                .totalPrice(totalPrice)
                                .build();
        }

        public com.example.DACN.dto.response.CartItemResponse updateCartItem(String userEmail, Long cartItemId,
                        com.example.DACN.dto.request.UpdateCartItemRequest request) {
                log.info("Updating cart item: {}, quantity: {} for user: {}", cartItemId, request.getQuantity(),
                                userEmail);

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                CartItem cartItem = cartItemRepository.findById(cartItemId)
                                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

                // Check if item belongs to the user
                if (!cartItem.getCart().getUser().getUserId().equals(user.getUserId())) {
                        throw new ResourceNotFoundException("Cart item not found in user's cart");
                }

                Product product = cartItem.getProduct();
                if (product.getStockQuantity() < request.getQuantity()) {
                        throw new IllegalStateException("Insufficient stock. Available: " + product.getStockQuantity());
                }

                cartItem.setQuantity(request.getQuantity());
                CartItem savedItem = cartItemRepository.save(cartItem);

                return cartMapper.toCartItemResponse(savedItem);
        }
}
