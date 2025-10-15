package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.LoanOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanOfficerRepository extends JpaRepository<LoanOfficer, Long> {
    
    Optional<LoanOfficer> findByUsername(String username);
    
    Optional<LoanOfficer> findByEmail(String email);
    
    Optional<LoanOfficer> findByUsernameOrEmail(String username, String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    List<LoanOfficer> findByLoanType(String loanType);
    
    @Query("SELECT lo FROM LoanOfficer lo WHERE lo.loanType = :loanType ORDER BY " +
           "(SELECT COUNT(oaa) FROM OfficerApplicationAssignment oaa WHERE oaa.officer = lo AND oaa.status IN ('PENDING', 'IN_PROGRESS')) ASC")
    List<LoanOfficer> findByLoanTypeOrderByWorkload(@Param("loanType") String loanType);
}
