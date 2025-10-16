package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ComplianceOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceOfficerRepository extends JpaRepository<ComplianceOfficer, Long> {
    
    Optional<ComplianceOfficer> findByUsername(String username);
    
    Optional<ComplianceOfficer> findByEmail(String email);
    
    Optional<ComplianceOfficer> findByUsernameOrEmail(String username, String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    List<ComplianceOfficer> findByLoanType(String loanType);
    
    @Query("SELECT co FROM ComplianceOfficer co ORDER BY " +
           "(SELECT COUNT(caa) FROM ComplianceOfficerApplicationAssignment caa WHERE caa.complianceOfficer = co AND caa.status IN ('PENDING', 'IN_PROGRESS')) ASC")
    List<ComplianceOfficer> findAllComplianceOfficersOrderByWorkload();
}
