package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicantReferenceRepository extends JpaRepository<ApplicantReference, Long> {
    List<ApplicantReference> findByApplicant_ApplicantId(Long applicantId);
}
