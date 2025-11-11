package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.entity.ExtractedField;
import com.tss.springsecurity.repository.ExtractedFieldRepository;
import com.tss.springsecurity.service.ExtractedFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExtractedFieldServiceImpl implements ExtractedFieldService {
    
    @Autowired
    private ExtractedFieldRepository extractedFieldRepository;
    
    @Override
    public List<ExtractedField> getExtractedFieldsByDocument(Long documentId) {
        return extractedFieldRepository.findByDocument_DocumentId(documentId);
    }
    
    @Override
    public List<ExtractedField> getExtractedFieldsByApplicant(Long applicantId) {
        return extractedFieldRepository.findByApplicantId(applicantId);
    }
    
    @Override
    public List<ExtractedField> getFieldValuesByApplicant(Long applicantId, String fieldName) {
        return extractedFieldRepository.findByApplicantIdAndFieldName(applicantId, fieldName);
    }
    
    @Override
    public ExtractedField updateFieldVerification(Long fieldId, Boolean verified, String verifiedBy) {
        ExtractedField field = extractedFieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Extracted field not found with ID: " + fieldId));
        
        field.setVerified(verified);
        field.setVerifiedBy(verifiedBy);
        field.setVerifiedAt(LocalDateTime.now());
        
        return extractedFieldRepository.save(field);
    }
    
    @Override
    public Map<String, Object> getExtractionSummary(Long documentId) {
        Long totalFields = extractedFieldRepository.countFieldsByDocumentId(documentId);
        Long verifiedFields = extractedFieldRepository.countVerifiedFieldsByDocumentId(documentId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalFields", totalFields);
        summary.put("verifiedFields", verifiedFields);
        summary.put("pendingFields", totalFields - verifiedFields);
        summary.put("verificationPercentage", totalFields > 0 ? (verifiedFields * 100.0 / totalFields) : 0);
        
        return summary;
    }
    
    @Override
    public Map<String, Object> getConsolidatedApplicantData(Long applicantId) {
        List<ExtractedField> allFields = extractedFieldRepository.findByApplicantId(applicantId);
        
        // Group by field name and get the most recent verified value, or most recent unverified if no verified exists
        Map<String, Object> consolidatedData = new HashMap<>();
        
        Map<String, List<ExtractedField>> fieldGroups = allFields.stream()
                .collect(Collectors.groupingBy(ExtractedField::getFieldName));
        
        for (Map.Entry<String, List<ExtractedField>> entry : fieldGroups.entrySet()) {
            String fieldName = entry.getKey();
            List<ExtractedField> fields = entry.getValue();
            
            // First try to find a verified field
            Optional<ExtractedField> verifiedField = fields.stream()
                    .filter(ExtractedField::getVerified)
                    .max(Comparator.comparing(ExtractedField::getCreatedAt));
            
            if (verifiedField.isPresent()) {
                consolidatedData.put(fieldName, createFieldData(verifiedField.get()));
            } else {
                // If no verified field, get the most recent one
                Optional<ExtractedField> latestField = fields.stream()
                        .max(Comparator.comparing(ExtractedField::getCreatedAt));
                
                if (latestField.isPresent()) {
                    consolidatedData.put(fieldName, createFieldData(latestField.get()));
                }
            }
        }
        
        return consolidatedData;
    }
    
    private Map<String, Object> createFieldData(ExtractedField field) {
        Map<String, Object> fieldData = new HashMap<>();
        fieldData.put("value", field.getFieldValue());
        fieldData.put("confidence", field.getConfidenceScore());
        fieldData.put("verified", field.getVerified());
        fieldData.put("extractionMethod", field.getExtractionMethod());
        fieldData.put("documentId", field.getDocument().getDocumentId());
        fieldData.put("extractedAt", field.getCreatedAt());
        
        if (field.getVerified()) {
            fieldData.put("verifiedBy", field.getVerifiedBy());
            fieldData.put("verifiedAt", field.getVerifiedAt());
        }
        
        return fieldData;
    }
}
