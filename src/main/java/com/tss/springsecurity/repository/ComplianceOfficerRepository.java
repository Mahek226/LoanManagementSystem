package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ComplianceOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplianceOfficerRepository extends JpaRepository<ComplianceOfficer, Long> {
    
    Optional<ComplianceOfficer> findByUsername(String username);
    
    Optional<ComplianceOfficer> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
}
