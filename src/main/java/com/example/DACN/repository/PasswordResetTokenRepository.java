package com.example.DACN.repository;

import com.example.DACN.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUserIdAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
            UUID userId, String otpCode, LocalDateTime currentTime);

    Optional<PasswordResetToken> findTopByUserIdAndIsUsedFalseOrderByCreatedAtDesc(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime currentTime);
}
