package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantFinancials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantFinancialsRepository extends JpaRepository<ApplicantFinancials, Long> {
    Optional<ApplicantFinancials> findByApplicant_ApplicantId(Long applicantId);
}
