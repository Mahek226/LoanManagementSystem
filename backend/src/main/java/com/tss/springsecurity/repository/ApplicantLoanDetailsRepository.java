package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantLoanDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicantLoanDetailsRepository extends JpaRepository<ApplicantLoanDetails, Long> {
    List<ApplicantLoanDetails> findByApplicant_ApplicantId(Long applicantId);
    List<ApplicantLoanDetails> findByStatus(String status);
    List<ApplicantLoanDetails> findByLoanStatusOrderBySubmittedAtDesc(String loanStatus);
    
    @Query("SELECT SUM(ald.loanAmount) FROM ApplicantLoanDetails ald")
    Double sumAllLoanAmounts();
}
