package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.PanDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PanDetailsRepository extends JpaRepository<PanDetails, Long> {
    List<PanDetails> findByApplicant_ApplicantId(Long applicantId);
}
