package com.example.DACN.service;

import com.example.DACN.dto.request.WishlistRequest;
import com.example.DACN.dto.response.DeleteWishlistResponse;
import com.example.DACN.dto.response.WishlistResponse;
import com.example.DACN.entity.Product;
import com.example.DACN.entity.User;
import com.example.DACN.entity.Wishlist;
import com.example.DACN.exception.DuplicateResourceException;
import com.example.DACN.exception.ResourceNotFoundException;
import com.example.DACN.mapper.WishlistMapper;
import com.example.DACN.repository.ProductRepository;
import com.example.DACN.repository.UserRepository;
import com.example.DACN.repository.WishlistRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;

    @Transactional
    public WishlistResponse createWishlist(WishlistRequest request, String userEmail) {
        log.info("Creating wishlist item for user: {} and product: {}", userEmail, request.getProductId());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"Active".equals(user.getStatus()) || Boolean.TRUE.equals(user.getHasDeleted())) {
            throw new ResourceNotFoundException("User is not active");
        }

        Product product = productRepository.findByProductIdAndHasDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (wishlistRepository.existsByUserUserIdAndProductProductId(user.getUserId(), product.getProductId())) {
            throw new DuplicateResourceException("Product is already in wishlist");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        log.info("Wishlist item created with ID: {}", savedWishlist.getWishlistId());

        return wishlistMapper.toWishlistResponse(savedWishlist);
    }

    @Transactional
    public DeleteWishlistResponse deleteWishlist(Long wishlistId, String userEmail) {
        log.info("Deleting wishlist item {} for user: {}", wishlistId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"Active".equals(user.getStatus()) || Boolean.TRUE.equals(user.getHasDeleted())) {
            throw new ResourceNotFoundException("User is not active");
        }

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        if (!wishlist.getUser().getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException("Wishlist item not found");
        }

        wishlistRepository.delete(wishlist);
        log.info("Wishlist item deleted with ID: {}", wishlistId);

        return wishlistMapper.toDeleteWishlistResponse(wishlist);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(String userEmail) {
        log.info("Fetching wishlist for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"Active".equals(user.getStatus()) || Boolean.TRUE.equals(user.getHasDeleted())) {
            throw new ResourceNotFoundException("User is not active");
        }

        List<Wishlist> wishlists = wishlistRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());

        return wishlists.stream()
                .map(wishlistMapper::toWishlistResponse)
                .collect(Collectors.toList());
    }
}
