package com.example.DACN.repository;

import com.example.DACN.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderIdAndHasDeletedFalse(Long orderId);

    Page<Order> findByUserUserIdAndHasDeletedFalse(UUID userId, Pageable pageable);

    Page<Order> findByShopShopIdAndHasDeletedFalse(Long shopId, Pageable pageable);

    List<Order> findByUserUserIdAndHasDeletedFalseOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId AND o.hasDeleted = false AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findUserOrdersByDateRange(@Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.shop.shopId = :shopId AND o.hasDeleted = false AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findShopOrdersByDateRange(@Param("shopId") Long shopId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByShopShopIdAndHasDeletedFalse(Long shopId);
}
