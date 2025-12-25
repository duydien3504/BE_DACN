package com.example.DACN.repository;

import com.example.DACN.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductProductIdAndHasDeletedFalse(Long productId, Pageable pageable);

    List<Review> findByUserUserIdAndHasDeletedFalse(UUID userId);

    Page<Review> findByProductProductIdAndRatingAndHasDeletedFalse(Long productId, Integer rating, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId AND r.hasDeleted = false")
    Double calculateAverageRating(@Param("productId") Long productId);

    long countByProductProductIdAndHasDeletedFalse(Long productId);

    boolean existsByUserUserIdAndProductProductId(UUID userId, Long productId);
}
