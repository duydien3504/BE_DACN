package com.example.DACN.repository;

import com.example.DACN.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserUserIdAndHasDeletedFalse(UUID userId);

    Optional<UserAddress> findByUserUserIdAndIsDefaultTrueAndHasDeletedFalse(UUID userId);

    List<UserAddress> findByUserUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);
}
