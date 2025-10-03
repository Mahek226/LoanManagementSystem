package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.CoApplicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoApplicantRepository extends JpaRepository<CoApplicant, Long> {
    List<CoApplicant> findByApplicant_ApplicantId(Long applicantId);
}
