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
@lombok.Getter
@lombok.Setter
@lombok.ToString
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
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
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
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    Set<ProductImage> images;

    @OneToMany(mappedBy = "product")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    Set<Wishlist> wishlists;

    @OneToMany(mappedBy = "product")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    Set<CartItem> cartItems;

    @OneToMany(mappedBy = "product")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    Set<OrderItem> orderItems;

    @OneToMany(mappedBy = "product")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    Set<Review> reviews;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Product))
            return false;
        Product product = (Product) o;
        return productId != null && productId.equals(product.productId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
