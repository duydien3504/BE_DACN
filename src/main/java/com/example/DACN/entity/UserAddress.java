package com.example.DACN.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_address_id")
    Long userAddressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "recipient_name", nullable = false, length = 100)
    String recipientName;

    @Column(nullable = false, length = 20)
    String phone;

    @Column(nullable = false, length = 100)
    String province;

    @Column(nullable = false, length = 100)
    String district;

    @Column(nullable = false, length = 100)
    String ward;

    @Column(name = "street_address", nullable = false)
    String streetAddress;

    @Column(name = "is_default", nullable = false)
    Boolean isDefault = false;

    @Column(name = "has_deleted", nullable = false)
    Boolean hasDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
