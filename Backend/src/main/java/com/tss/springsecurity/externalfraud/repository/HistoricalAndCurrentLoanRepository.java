package com.tss.springsecurity.externalfraud.repository;

import com.tss.springsecurity.externalfraud.entity.HistoricalAndCurrentLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface HistoricalAndCurrentLoanRepository extends JpaRepository<HistoricalAndCurrentLoan, Long> {
    
    List<HistoricalAndCurrentLoan> findByPersonId(Long personId);
    
    List<HistoricalAndCurrentLoan> findByPersonIdAndStatus(Long personId, String status);
    
    List<HistoricalAndCurrentLoan> findByPersonIdAndDefaultFlagTrue(Long personId);
    
    @Query("SELECT l FROM HistoricalAndCurrentLoan l WHERE l.personId = :personId AND l.status = 'ACTIVE'")
    List<HistoricalAndCurrentLoan> findActiveLoans(@Param("personId") Long personId);
    
    @Query("SELECT SUM(l.outstandingBalance) FROM HistoricalAndCurrentLoan l WHERE l.personId = :personId AND l.status = 'ACTIVE'")
    BigDecimal getTotalOutstandingBalance(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(l) FROM HistoricalAndCurrentLoan l WHERE l.personId = :personId AND l.defaultFlag = true")
    long countDefaultedLoans(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(l) FROM HistoricalAndCurrentLoan l WHERE l.personId = :personId AND l.status = 'ACTIVE'")
    long countActiveLoans(@Param("personId") Long personId);
    
    @Query("SELECT l FROM HistoricalAndCurrentLoan l WHERE l.personId = :personId ORDER BY l.startDate DESC")
    List<HistoricalAndCurrentLoan> findByPersonIdOrderByStartDateDesc(@Param("personId") Long personId);
}
