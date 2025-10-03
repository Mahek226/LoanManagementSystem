package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.LoanCollateral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanCollateralRepository extends JpaRepository<LoanCollateral, Long> {
    List<LoanCollateral> findByLoan_LoanId(Long loanId);
}
