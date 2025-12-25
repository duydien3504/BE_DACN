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
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(nullable = false, length = 200)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    Integer stockQuantity = 0;

    @Column(name = "sold_count", nullable = false)
    Integer soldCount = 0;

    @Column(nullable = false, length = 20)
    String status = "Active"; // Active/Inactive/Banned

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    Set<ProductImage> images;

    @OneToMany(mappedBy = "product")
    Set<Wishlist> wishlists;

    @OneToMany(mappedBy = "product")
    Set<CartItem> cartItems;

    @OneToMany(mappedBy = "product")
    Set<OrderItem> orderItems;

    @OneToMany(mappedBy = "product")
    Set<Review> reviews;
}
