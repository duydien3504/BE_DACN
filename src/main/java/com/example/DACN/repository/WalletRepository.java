package com.example.DACN.repository;

import com.example.DACN.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserUserId(UUID userId);

    @Query("SELECT w FROM Wallet w WHERE w.user IS NULL")
    Optional<Wallet> findSystemWallet();

    boolean existsByUserUserId(UUID userId);
}
