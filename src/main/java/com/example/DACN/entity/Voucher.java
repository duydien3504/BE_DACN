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
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    Long voucherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    Shop shop; // Null if platform voucher (Admin)

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    String discountType; // PERCENT/FIXED

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 15, scale = 2)
    BigDecimal minOrderValue;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    LocalDateTime endDate;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "voucher")
    Set<UserVoucher> userVouchers;
}
