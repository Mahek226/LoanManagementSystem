package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantPropertyDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantPropertyDetailsRepository extends JpaRepository<ApplicantPropertyDetails, Long> {
    Optional<ApplicantPropertyDetails> findByApplicant_ApplicantId(Long applicantId);
}
