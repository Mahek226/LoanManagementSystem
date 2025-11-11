package com.tss.springsecurity.externalfraud.repository;

import com.tss.springsecurity.externalfraud.entity.BankRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BankRecordRepository extends JpaRepository<BankRecord, Long> {
    
    List<BankRecord> findByPersonId(Long personId);
    
    List<BankRecord> findByPersonIdAndIsActiveTrue(Long personId);
    
    @Query("SELECT SUM(b.balanceAmount) FROM BankRecord b WHERE b.personId = :personId AND b.isActive = true")
    BigDecimal getTotalActiveBalance(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(b) FROM BankRecord b WHERE b.personId = :personId AND b.isActive = true")
    long countActiveAccounts(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(b) FROM BankRecord b WHERE b.personId = :personId AND b.isActive = false")
    long countInactiveAccounts(@Param("personId") Long personId);
    
    List<BankRecord> findByPersonIdAndAccountType(Long personId, String accountType);
}
