package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.OfficerApplicationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerApplicationAssignmentRepository extends JpaRepository<OfficerApplicationAssignment, Long> {
    
    List<OfficerApplicationAssignment> findByOfficer_OfficerId(Long officerId);
    
    List<OfficerApplicationAssignment> findByApplicant_ApplicantId(Long applicantId);
    
    List<OfficerApplicationAssignment> findByStatus(String status);
    
    Optional<OfficerApplicationAssignment> findByApplicant_ApplicantIdAndStatus(Long applicantId, String status);
    
    @Query("SELECT COUNT(a) FROM OfficerApplicationAssignment a WHERE a.officer.officerId = :officerId AND a.status IN ('PENDING', 'IN_PROGRESS')")
    Long countActiveAssignmentsByOfficer(@Param("officerId") Long officerId);
    
    @Query("SELECT a FROM OfficerApplicationAssignment a WHERE a.officer.loanType = :loanType AND a.status IN ('PENDING', 'IN_PROGRESS') GROUP BY a.officer ORDER BY COUNT(a.officer) ASC")
    List<OfficerApplicationAssignment> findOfficersWithLeastWorkloadByLoanType(@Param("loanType") String loanType);
}
