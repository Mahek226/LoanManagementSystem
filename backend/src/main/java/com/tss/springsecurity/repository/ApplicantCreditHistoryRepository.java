package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantCreditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantCreditHistoryRepository extends JpaRepository<ApplicantCreditHistory, Long> {
    Optional<ApplicantCreditHistory> findByApplicant_ApplicantId(Long applicantId);
}
