package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.PassportDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassportDetailsRepository extends JpaRepository<PassportDetails, Long> {
    List<PassportDetails> findByApplicant_ApplicantId(Long applicantId);
}
