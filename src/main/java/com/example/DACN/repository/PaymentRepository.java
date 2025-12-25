package com.example.DACN.repository;

import com.example.DACN.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderOrderId(Long orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    List<Payment> findByStatus(String status);

    boolean existsByTransactionCode(String transactionCode);

    Optional<Payment> findByTransactionCodeAndStatus(String transactionCode, String status);
}
