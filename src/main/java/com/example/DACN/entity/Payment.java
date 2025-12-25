package com.example.DACN.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    Long paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true, unique = true)
    Order order;

    @Column(name = "transaction_code", unique = true, length = 100)
    String transactionCode; // Code from MOMO

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(nullable = false, length = 20)
    String status; // Success/Failed

    @Column(name = "payment_time")
    LocalDateTime paymentTime;
}
