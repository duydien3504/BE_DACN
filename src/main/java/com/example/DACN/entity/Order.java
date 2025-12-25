package com.example.DACN.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user; // Buyer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    Shop shop; // Seller

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 15, scale = 2)
    BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "voucher_discount", precision = 15, scale = 2)
    BigDecimal voucherDiscount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal finalAmount;

    @Column(name = "payment_method", nullable = false, length = 20)
    String paymentMethod; // MOMO/COD

    @Column(columnDefinition = "TEXT")
    String note;

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false; // Order cancellation

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    Set<OrderItem> orderItems;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    Set<OrderStatusHistory> statusHistories;

    @OneToOne(mappedBy = "order")
    Payment payment;
}
