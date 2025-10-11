package com.tss.springsecurity.externalfraud.repository;

import com.tss.springsecurity.externalfraud.entity.GovernmentIssuedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GovernmentIssuedDocumentRepository extends JpaRepository<GovernmentIssuedDocument, Long> {
    
    List<GovernmentIssuedDocument> findByPersonId(Long personId);
    
    List<GovernmentIssuedDocument> findByPersonIdAndDocumentType(Long personId, String documentType);
    
    Optional<GovernmentIssuedDocument> findByDocumentNumber(String documentNumber);
    
    @Query("SELECT d FROM GovernmentIssuedDocument d WHERE d.personId = :personId AND d.verificationStatus = :status")
    List<GovernmentIssuedDocument> findByPersonIdAndVerificationStatus(@Param("personId") Long personId, 
                                                                       @Param("status") String verificationStatus);
    
    @Query("SELECT COUNT(d) FROM GovernmentIssuedDocument d WHERE d.personId = :personId AND d.verificationStatus = 'VERIFIED'")
    long countVerifiedDocuments(@Param("personId") Long personId);
    
    @Query("SELECT COUNT(d) FROM GovernmentIssuedDocument d WHERE d.personId = :personId AND d.verificationStatus = 'EXPIRED'")
    long countExpiredDocuments(@Param("personId") Long personId);
    
    @Query("SELECT d FROM GovernmentIssuedDocument d WHERE d.expiryDate < CURRENT_DATE AND d.verificationStatus != 'EXPIRED'")
    List<GovernmentIssuedDocument> findExpiredDocuments();
}
