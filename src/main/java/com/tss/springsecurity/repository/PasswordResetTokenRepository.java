package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByResetTokenAndIsUsedFalseAndExpiresAtAfter(
            String resetToken, LocalDateTime currentTime);
    
    Optional<PasswordResetToken> findByEmailAndIsUsedFalseAndExpiresAtAfter(
            String email, LocalDateTime currentTime);
    
    void deleteByEmailAndExpiresAtBefore(String email, LocalDateTime currentTime);
}
