package com.example.DACN.repository;

import com.example.DACN.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserUserId(UUID userId);

    boolean existsByUserUserId(UUID userId);
}
