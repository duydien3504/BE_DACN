package com.example.DACN.repository;

import com.example.DACN.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeAndHasDeletedFalse(String code);

    List<Voucher> findByShopIsNullAndHasDeletedFalse(); // Platform vouchers

    List<Voucher> findByShopShopIdAndHasDeletedFalse(Long shopId);

    @Query("SELECT v FROM Voucher v WHERE v.hasDeleted = false AND v.startDate <= :now AND v.endDate >= :now AND v.quantity > 0")
    List<Voucher> findActiveVouchers(@Param("now") LocalDateTime now);

    @Query("SELECT v FROM Voucher v WHERE v.shop.shopId = :shopId AND v.hasDeleted = false AND v.startDate <= :now AND v.endDate >= :now AND v.quantity > 0")
    List<Voucher> findActiveShopVouchers(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);

    boolean existsByCode(String code);
}
