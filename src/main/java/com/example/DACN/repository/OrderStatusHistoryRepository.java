package com.example.DACN.repository;

import com.example.DACN.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<OrderStatusHistory> findTopByOrderOrderIdOrderByCreatedAtDesc(Long orderId);

    List<OrderStatusHistory> findByOrderOrderIdAndStatus(Long orderId, String status);
}
