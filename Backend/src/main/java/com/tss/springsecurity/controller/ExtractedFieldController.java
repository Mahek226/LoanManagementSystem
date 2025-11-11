package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.ExtractedField;
import com.tss.springsecurity.service.ExtractedFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/extracted-fields")
public class ExtractedFieldController {
    
    @Autowired
    private ExtractedFieldService extractedFieldService;
    
    @GetMapping("/document/{documentId}")
    public ResponseEntity<Map<String, Object>> getExtractedFieldsByDocument(@PathVariable Long documentId) {
        try {
            List<ExtractedField> fields = extractedFieldService.getExtractedFieldsByDocument(documentId);
            Map<String, Object> summary = extractedFieldService.getExtractionSummary(documentId);
            
            List<Map<String, Object>> fieldDetails = new ArrayList<>();
            for (ExtractedField field : fields) {
                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("fieldId", field.getFieldId());
                fieldInfo.put("fieldName", field.getFieldName());
                fieldInfo.put("fieldValue", field.getFieldValue());
                fieldInfo.put("confidenceScore", field.getConfidenceScore());
                fieldInfo.put("extractionMethod", field.getExtractionMethod());
                fieldInfo.put("verified", field.getVerified());
                fieldInfo.put("verifiedBy", field.getVerifiedBy());
                fieldInfo.put("verifiedAt", field.getVerifiedAt());
                fieldInfo.put("createdAt", field.getCreatedAt());
                fieldDetails.add(fieldInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", documentId);
            response.put("fields", fieldDetails);
            response.put("summary", summary);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve extracted fields: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Map<String, Object>> getExtractedFieldsByApplicant(@PathVariable Long applicantId) {
        try {
            List<ExtractedField> fields = extractedFieldService.getExtractedFieldsByApplicant(applicantId);
            Map<String, Object> consolidatedData = extractedFieldService.getConsolidatedApplicantData(applicantId);
            
            List<Map<String, Object>> fieldDetails = new ArrayList<>();
            for (ExtractedField field : fields) {
                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("fieldId", field.getFieldId());
                fieldInfo.put("documentId", field.getDocument().getDocumentId());
                fieldInfo.put("fieldName", field.getFieldName());
                fieldInfo.put("fieldValue", field.getFieldValue());
                fieldInfo.put("confidenceScore", field.getConfidenceScore());
                fieldInfo.put("extractionMethod", field.getExtractionMethod());
                fieldInfo.put("verified", field.getVerified());
                fieldInfo.put("verifiedBy", field.getVerifiedBy());
                fieldInfo.put("verifiedAt", field.getVerifiedAt());
                fieldInfo.put("createdAt", field.getCreatedAt());
                fieldDetails.add(fieldInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("allFields", fieldDetails);
            response.put("consolidatedData", consolidatedData);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve extracted fields: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/applicant/{applicantId}/field/{fieldName}")
    public ResponseEntity<Map<String, Object>> getFieldValuesByApplicant(
            @PathVariable Long applicantId, 
            @PathVariable String fieldName) {
        try {
            List<ExtractedField> fields = extractedFieldService.getFieldValuesByApplicant(applicantId, fieldName);
            
            List<Map<String, Object>> fieldDetails = new ArrayList<>();
            for (ExtractedField field : fields) {
                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("fieldId", field.getFieldId());
                fieldInfo.put("documentId", field.getDocument().getDocumentId());
                fieldInfo.put("fieldValue", field.getFieldValue());
                fieldInfo.put("confidenceScore", field.getConfidenceScore());
                fieldInfo.put("extractionMethod", field.getExtractionMethod());
                fieldInfo.put("verified", field.getVerified());
                fieldInfo.put("createdAt", field.getCreatedAt());
                fieldDetails.add(fieldInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("fieldName", fieldName);
            response.put("fields", fieldDetails);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve field values: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{fieldId}/verify")
    public ResponseEntity<Map<String, Object>> updateFieldVerification(
            @PathVariable Long fieldId,
            @RequestParam("verified") Boolean verified,
            @RequestParam("verifiedBy") String verifiedBy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            ExtractedField updatedField = extractedFieldService.updateFieldVerification(fieldId, verified, verifiedBy);
            
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("fieldId", updatedField.getFieldId());
            fieldInfo.put("fieldName", updatedField.getFieldName());
            fieldInfo.put("fieldValue", updatedField.getFieldValue());
            fieldInfo.put("verified", updatedField.getVerified());
            fieldInfo.put("verifiedBy", updatedField.getVerifiedBy());
            fieldInfo.put("verifiedAt", updatedField.getVerifiedAt());
            
            response.put("success", true);
            response.put("message", "Field verification status updated successfully");
            response.put("field", fieldInfo);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update field verification: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/applicant/{applicantId}/consolidated")
    public ResponseEntity<Map<String, Object>> getConsolidatedApplicantData(@PathVariable Long applicantId) {
        try {
            Map<String, Object> consolidatedData = extractedFieldService.getConsolidatedApplicantData(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("data", consolidatedData);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve consolidated data: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
