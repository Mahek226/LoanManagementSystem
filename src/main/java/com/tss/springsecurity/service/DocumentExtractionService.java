package com.tss.springsecurity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionService {

    private final RestTemplate restTemplate;

    @Value("${document.extraction.service.url:http://127.0.0.1:8000}")
    private String extractionServiceUrl;

    /**
     * Extract data from a document using the Python extraction service
     * 
     * @param file The document file to extract data from
     * @param documentType The type of document (AADHAAR, PAN, PASSPORT, etc.)
     * @param applicantId The ID of the applicant
     * @return Map containing extracted fields
     */
    public Map<String, Object> extractDocumentData(MultipartFile file, String documentType, Long applicantId) {
        try {
            log.info("Sending document for extraction: type={}, applicantId={}, filename={}", 
                    documentType, applicantId, file.getOriginalFilename());

            // Prepare the request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("document_type", documentType);
            body.add("applicant_id", applicantId.toString());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call the Python extraction service
            String url = extractionServiceUrl + "/extract";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Document extraction successful for applicantId={}, documentType={}", 
                        applicantId, documentType);
                return response.getBody();
            } else {
                log.error("Document extraction failed with status: {}", response.getStatusCode());
                return createErrorResponse("Extraction service returned error status");
            }

        } catch (Exception e) {
            log.error("Error during document extraction: {}", e.getMessage(), e);
            return createErrorResponse("Error extracting document: " + e.getMessage());
        }
    }

    /**
     * Extract data from multiple documents
     * 
     * @param files Array of document files
     * @param documentTypes Array of document types corresponding to files
     * @param applicantId The ID of the applicant
     * @return Map of document types to extracted data
     */
    public Map<String, Map<String, Object>> extractMultipleDocuments(
            MultipartFile[] files, 
            String[] documentTypes, 
            Long applicantId) {
        
        Map<String, Map<String, Object>> results = new HashMap<>();
        
        for (int i = 0; i < files.length && i < documentTypes.length; i++) {
            String docType = documentTypes[i];
            MultipartFile file = files[i];
            
            try {
                Map<String, Object> extractedData = extractDocumentData(file, docType, applicantId);
                results.put(docType, extractedData);
            } catch (Exception e) {
                log.error("Failed to extract document type {}: {}", docType, e.getMessage());
                results.put(docType, createErrorResponse("Extraction failed: " + e.getMessage()));
            }
        }
        
        return results;
    }

    /**
     * Check if the extraction service is available
     * 
     * @return true if service is reachable, false otherwise
     */
    public boolean isExtractionServiceAvailable() {
        try {
            String url = extractionServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Extraction service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate extracted data against expected fields
     * 
     * @param extractedData The extracted data map
     * @param documentType The type of document
     * @return true if data is valid, false otherwise
     */
    public boolean validateExtractedData(Map<String, Object> extractedData, String documentType) {
        if (extractedData == null || extractedData.isEmpty()) {
            return false;
        }

        // Check if extraction was successful
        if (extractedData.containsKey("error")) {
            return false;
        }

        // Get the extracted fields
        @SuppressWarnings("unchecked")
        Map<String, Object> extracted = (Map<String, Object>) extractedData.get("extracted");
        
        if (extracted == null || extracted.isEmpty()) {
            return false;
        }

        // Validate based on document type
        switch (documentType.toUpperCase()) {
            case "AADHAAR":
            case "AADHAAR_CARD":
                return extracted.containsKey("name") && extracted.containsKey("aadhaar_number");
            
            case "PAN":
            case "PAN_CARD":
                return extracted.containsKey("name") && extracted.containsKey("pan_number");
            
            case "PASSPORT":
                return extracted.containsKey("name") && extracted.containsKey("passport_no");
            
            case "DRIVING_LICENSE":
            case "DL":
                return extracted.containsKey("name") && extracted.containsKey("dl_number");
            
            case "BANK_STATEMENT":
                return extracted.containsKey("account_number") || extracted.containsKey("bank_name");
            
            default:
                // For other document types, just check if we have some extracted data
                return !extracted.isEmpty();
        }
    }

    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        errorResponse.put("success", false);
        return errorResponse;
    }
}
