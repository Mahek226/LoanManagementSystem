package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ComplianceOfficerApplicationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceOfficerApplicationAssignmentRepository extends JpaRepository<ComplianceOfficerApplicationAssignment, Long> {
    
    List<ComplianceOfficerApplicationAssignment> findByComplianceOfficer_OfficerId(Long officerId);
    
    List<ComplianceOfficerApplicationAssignment> findByApplicant_ApplicantId(Long applicantId);
    
    List<ComplianceOfficerApplicationAssignment> findByStatus(String status);
    
    Optional<ComplianceOfficerApplicationAssignment> findByApplicant_ApplicantIdAndStatus(Long applicantId, String status);
    
    @Query("SELECT COUNT(c) FROM ComplianceOfficerApplicationAssignment c WHERE c.complianceOfficer.officerId = :officerId AND c.status IN ('PENDING', 'IN_PROGRESS')")
    Long countActiveAssignmentsByOfficer(@Param("officerId") Long officerId);
    
    // New methods using loan_id relationship
    List<ComplianceOfficerApplicationAssignment> findByLoan_LoanId(Long loanId);
    
    Optional<ComplianceOfficerApplicationAssignment> findByLoan_LoanIdAndStatus(Long loanId, String status);
    
    @Query("SELECT c FROM ComplianceOfficerApplicationAssignment c WHERE c.loan.loanId = :loanId ORDER BY c.assignedAt DESC")
    List<ComplianceOfficerApplicationAssignment> findByLoanIdOrderByAssignedAtDesc(@Param("loanId") Long loanId);
}
