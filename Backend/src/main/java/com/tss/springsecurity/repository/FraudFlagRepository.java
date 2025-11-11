package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.FraudFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {
    List<FraudFlag> findByApplicant_ApplicantId(Long applicantId);
    List<FraudFlag> findByLoan_LoanId(Long loanId);
    List<FraudFlag> findBySeverity(Integer severity);
}
