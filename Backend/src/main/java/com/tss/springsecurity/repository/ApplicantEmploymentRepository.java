package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantEmployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicantEmploymentRepository extends JpaRepository<ApplicantEmployment, Long> {
    Optional<ApplicantEmployment> findByApplicant_ApplicantId(Long applicantId);
}
