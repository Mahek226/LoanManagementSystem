package com.tss.springsecurity.externalfraud.controller;

import com.tss.springsecurity.externalfraud.service.BulkDataInsertionService;
import com.tss.springsecurity.externalfraud.service.BulkDataInsertionService.BulkDataRequest;
import com.tss.springsecurity.externalfraud.service.BulkDataInsertionService.BulkInsertionResult;
import com.tss.springsecurity.externalfraud.service.BulkDataInsertionService.DatabaseStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/external-fraud/bulk")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BulkDataController {
    
    private final BulkDataInsertionService bulkDataService;
    
    /**
     * Bulk insert data into all external fraud database tables
     */
    @PostMapping("/insert")
    public ResponseEntity<?> bulkInsertData(@RequestBody BulkDataRequest request) {
        try {
            log.info("Received bulk data insertion request");
            
            // Validate request
            if (request == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Request body cannot be null"));
            }
            
            // Check if at least one data type is provided
            boolean hasData = (request.getPersons() != null && !request.getPersons().isEmpty()) ||
                             (request.getCriminalRecords() != null && !request.getCriminalRecords().isEmpty()) ||
                             (request.getBankRecords() != null && !request.getBankRecords().isEmpty()) ||
                             (request.getLoans() != null && !request.getLoans().isEmpty()) ||
                             (request.getGovernmentDocuments() != null && !request.getGovernmentDocuments().isEmpty());
            
            if (!hasData) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("At least one data type must be provided"));
            }
            
            BulkInsertionResult result = bulkDataService.insertBulkData(request);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error in bulk data insertion API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Get database statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DatabaseStats> getDatabaseStats() {
        try {
            DatabaseStats stats = bulkDataService.getDatabaseStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting database stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Clear all data from external fraud database
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAllData() {
        try {
            log.info("Clearing all external fraud database data");
            bulkDataService.clearAllData();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "All external fraud database data cleared successfully");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error clearing database: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Bulk Data Insertion Service");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("status", "failed");
        error.put("timestamp", java.time.LocalDateTime.now().toString());
        return error;
    }
}
