package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantBasicDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantBasicDetailsRepository extends JpaRepository<ApplicantBasicDetails, Long> {
    Optional<ApplicantBasicDetails> findByApplicant_ApplicantId(Long applicantId);
}
