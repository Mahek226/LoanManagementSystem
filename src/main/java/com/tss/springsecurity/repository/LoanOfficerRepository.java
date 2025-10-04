package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.LoanOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanOfficerRepository extends JpaRepository<LoanOfficer, Long> {
    
    Optional<LoanOfficer> findByUsername(String username);
    
    Optional<LoanOfficer> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
}
