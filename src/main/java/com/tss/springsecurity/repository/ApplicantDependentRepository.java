package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantDependent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicantDependentRepository extends JpaRepository<ApplicantDependent, Long> {
    List<ApplicantDependent> findByApplicant_ApplicantId(Long applicantId);
}
