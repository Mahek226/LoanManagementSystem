package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.AadhaarDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AadhaarDetailsRepository extends JpaRepository<AadhaarDetails, Long> {
    List<AadhaarDetails> findByApplicant_ApplicantId(Long applicantId);
}
