package com.example.DACN.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    UUID userId;

    @Column(nullable = false, unique = true, length = 100)
    String email;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(name = "full_name", length = 100)
    String fullName;

    @Column(name = "phone_number", length = 20)
    String phoneNumber;

    @Column(name = "avatar_url")
    String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    Role role;

    @Column(name = "is_email_verified", nullable = false)
    Boolean isEmailVerified = false;

    @Column(nullable = false, length = 20)
    String status = "Active"; // Active/Banned

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    Set<UserAddress> addresses;

    @OneToMany(mappedBy = "user")
    Set<Shop> shops;

    @OneToMany(mappedBy = "user")
    Set<Wishlist> wishlists;

    @OneToMany(mappedBy = "user")
    Set<Order> orders;

    @OneToMany(mappedBy = "user")
    Set<Review> reviews;

    @OneToOne(mappedBy = "user")
    Cart cart;

    @OneToOne(mappedBy = "user")
    Wallet wallet;
}
