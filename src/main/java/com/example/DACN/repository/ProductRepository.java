package com.example.DACN.repository;

import com.example.DACN.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductIdAndHasDeletedFalse(Long productId);

    Page<Product> findByShopShopIdAndHasDeletedFalse(Long shopId, Pageable pageable);

    Page<Product> findByCategoryCategoryIdAndHasDeletedFalse(Long categoryId, Pageable pageable);

    Page<Product> findByStatusAndHasDeletedFalse(String status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.hasDeleted = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.hasDeleted = false ORDER BY p.soldCount DESC")
    Page<Product> findBestSellers(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.hasDeleted = false ORDER BY p.createdAt DESC")
    Page<Product> findNewestProducts(Pageable pageable);

    List<Product> findTop10ByShopShopIdAndHasDeletedFalseOrderBySoldCountDesc(Long shopId);

    @Query("SELECT p FROM Product p WHERE p.hasDeleted = false " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
            "AND (:shopId IS NULL OR p.shop.shopId = :shopId)")
    Page<Product> findProductsWithFilters(
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("categoryId") Long categoryId,
            @Param("shopId") Long shopId,
            Pageable pageable);
}
