package com.example.DACN.repository;

import com.example.DACN.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByShopIdAndHasDeletedFalse(Long shopId);

    List<Shop> findByUserUserIdAndHasDeletedFalse(UUID userId);

    List<Shop> findByIsApprovedTrueAndHasDeletedFalse();

    List<Shop> findByIsApprovedFalseAndHasDeletedFalse();

    @Query("SELECT s FROM Shop s WHERE s.hasDeleted = false ORDER BY s.ratingAvg DESC")
    List<Shop> findTopRatedShops();

    Optional<Shop> findByUserUserId(UUID userId);
}
