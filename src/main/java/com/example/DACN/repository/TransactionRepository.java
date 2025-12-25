package com.example.DACN.repository;

import com.example.DACN.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByWalletWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    List<Transaction> findByWalletWalletIdAndType(Long walletId, String type);

    List<Transaction> findByOrderOrderId(Long orderId);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.walletId = :walletId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByWalletAndDateRange(@Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
