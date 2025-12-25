package com.example.DACN.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shop_id")
    Long shopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user; // Shop owner

    @Column(name = "shop_name", nullable = false, length = 200)
    String shopName;

    @Column(name = "shop_description", columnDefinition = "TEXT")
    String shopDescription;

    @Column(name = "logo_url")
    String logoUrl;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "is_approved", nullable = false)
    Boolean isApproved = false;

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shop")
    Set<Product> products;

    @OneToMany(mappedBy = "shop")
    Set<Order> orders;

    @OneToMany(mappedBy = "shop")
    Set<Voucher> vouchers;
}
