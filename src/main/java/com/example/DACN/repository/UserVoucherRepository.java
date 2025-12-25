package com.example.DACN.repository;

import com.example.DACN.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    List<UserVoucher> findByUserUserIdAndIsUsedFalse(UUID userId);

    List<UserVoucher> findByUserUserId(UUID userId);

    Optional<UserVoucher> findByUserUserIdAndVoucherVoucherIdAndIsUsedFalse(UUID userId, Long voucherId);

    boolean existsByUserUserIdAndVoucherVoucherId(UUID userId, Long voucherId);

    long countByVoucherVoucherIdAndIsUsedTrue(Long voucherId);
}
