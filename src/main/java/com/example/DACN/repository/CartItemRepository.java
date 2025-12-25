package com.example.DACN.repository;

import com.example.DACN.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartCartId(Long cartId);

    Optional<CartItem> findByCartCartIdAndProductProductId(Long cartId, Long productId);

    void deleteByCartCartId(Long cartId);

    void deleteByCartCartIdAndProductProductId(Long cartId, Long productId);

    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.cartId = :cartId")
    Long countItemsByCartId(Long cartId);
}
