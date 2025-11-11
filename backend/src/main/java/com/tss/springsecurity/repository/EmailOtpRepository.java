package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    
    Optional<EmailOtp> findByEmailAndOtpCodeAndIsVerifiedFalseAndExpiresAtAfter(
            String email, String otpCode, LocalDateTime currentTime);
    
    Optional<EmailOtp> findFirstByEmailAndIsVerifiedFalseOrderByCreatedAtDesc(String email);
    
    void deleteByExpiresAtBefore(LocalDateTime currentTime);
}
