package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ExtractedField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractedFieldRepository extends JpaRepository<ExtractedField, Long> {
    
    /**
     * Find all extracted fields for a specific document
     */
    List<ExtractedField> findByDocument_DocumentId(Long documentId);
    
    /**
     * Find extracted fields by document and field name
     */
    List<ExtractedField> findByDocument_DocumentIdAndFieldName(Long documentId, String fieldName);
    
    /**
     * Find verified extracted fields for a document
     */
    List<ExtractedField> findByDocument_DocumentIdAndVerified(Long documentId, Boolean verified);
    
    /**
     * Find extracted fields by extraction method
     */
    List<ExtractedField> findByExtractionMethod(String extractionMethod);
    
    /**
     * Count extracted fields for a document
     */
    @Query("SELECT COUNT(ef) FROM ExtractedField ef WHERE ef.document.documentId = :documentId")
    Long countFieldsByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * Count verified fields for a document
     */
    @Query("SELECT COUNT(ef) FROM ExtractedField ef WHERE ef.document.documentId = :documentId AND ef.verified = true")
    Long countVerifiedFieldsByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * Find all extracted fields for an applicant (across all documents)
     */
    @Query("SELECT ef FROM ExtractedField ef WHERE ef.document.applicant.applicantId = :applicantId")
    List<ExtractedField> findByApplicantId(@Param("applicantId") Long applicantId);
    
    /**
     * Find specific field value for an applicant by field name
     */
    @Query("SELECT ef FROM ExtractedField ef WHERE ef.document.applicant.applicantId = :applicantId AND ef.fieldName = :fieldName")
    List<ExtractedField> findByApplicantIdAndFieldName(@Param("applicantId") Long applicantId, @Param("fieldName") String fieldName);
}
