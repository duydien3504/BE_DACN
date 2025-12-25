package com.example.DACN.repository;

import com.example.DACN.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Wishlist> findByUserUserIdAndProductProductId(UUID userId, Long productId);

    boolean existsByUserUserIdAndProductProductId(UUID userId, Long productId);

    void deleteByUserUserIdAndProductProductId(UUID userId, Long productId);

    long countByUserUserId(UUID userId);
}
