package com.tss.springsecurity.externalfraud.repository;

import com.tss.springsecurity.externalfraud.entity.CriminalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriminalRecordRepository extends JpaRepository<CriminalRecord, Long> {
    
    List<CriminalRecord> findByPersonId(Long personId);
    
    List<CriminalRecord> findByPersonIdAndStatus(Long personId, String status);
    
    @Query("SELECT c FROM CriminalRecord c WHERE c.personId = :personId AND c.status IN ('OPEN', 'CONVICTED')")
    List<CriminalRecord> findActiveCriminalRecords(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(c) FROM CriminalRecord c WHERE c.personId = :personId")
    long countByPersonId(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(c) FROM CriminalRecord c WHERE c.personId = :personId AND c.status = 'CONVICTED'")
    long countConvictedCases(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(c) FROM CriminalRecord c WHERE c.personId = :personId AND c.status = 'OPEN'")
    long countOpenCases(@Param("personId") Long personId);
    
    @Query("SELECT c FROM CriminalRecord c WHERE c.personId = :personId AND c.caseType LIKE %:caseType%")
    List<CriminalRecord> findByCaseTypeContaining(@Param("personId") Long personId, @Param("caseType") String caseType);
}
