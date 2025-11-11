package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.ExtractedField;

import java.util.List;
import java.util.Map;

public interface ExtractedFieldService {
    
    /**
     * Get all extracted fields for a document
     * @param documentId Document ID
     * @return List of extracted fields
     */
    List<ExtractedField> getExtractedFieldsByDocument(Long documentId);
    
    /**
     * Get extracted fields for an applicant across all documents
     * @param applicantId Applicant ID
     * @return List of extracted fields
     */
    List<ExtractedField> getExtractedFieldsByApplicant(Long applicantId);
    
    /**
     * Get specific field value for an applicant
     * @param applicantId Applicant ID
     * @param fieldName Field name (e.g., "PAN_Number", "Name", "DOB")
     * @return List of extracted field values
     */
    List<ExtractedField> getFieldValuesByApplicant(Long applicantId, String fieldName);
    
    /**
     * Update verification status of an extracted field
     * @param fieldId Field ID
     * @param verified Verification status
     * @param verifiedBy Who verified the field
     * @return Updated field
     */
    ExtractedField updateFieldVerification(Long fieldId, Boolean verified, String verifiedBy);
    
    /**
     * Get extraction summary for a document
     * @param documentId Document ID
     * @return Summary with counts and verification status
     */
    Map<String, Object> getExtractionSummary(Long documentId);
    
    /**
     * Get consolidated applicant data from all extracted fields
     * This method aggregates all verified extracted data for an applicant
     * @param applicantId Applicant ID
     * @return Map of field names to values
     */
    Map<String, Object> getConsolidatedApplicantData(Long applicantId);
}
