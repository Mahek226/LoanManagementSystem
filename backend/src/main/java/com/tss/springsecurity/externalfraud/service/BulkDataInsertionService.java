package com.tss.springsecurity.externalfraud.service;

import com.tss.springsecurity.externalfraud.entity.*;
import com.tss.springsecurity.externalfraud.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkDataInsertionService {
    
    private final PersonRepository personRepository;
    private final CriminalRecordRepository criminalRecordRepository;
    private final BankRecordRepository bankRecordRepository;
    private final HistoricalAndCurrentLoanRepository loanRepository;
    private final GovernmentIssuedDocumentRepository documentRepository;
    
    @Transactional
    public BulkInsertionResult insertBulkData(BulkDataRequest request) {
        log.info("Starting bulk data insertion process");
        
        BulkInsertionResult result = new BulkInsertionResult();
        Map<String, Integer> insertedCounts = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        try {
            // Insert Persons first (as they are referenced by other entities)
            if (request.getPersons() != null && !request.getPersons().isEmpty()) {
                log.info("Inserting {} persons", request.getPersons().size());
                List<Person> savedPersons = personRepository.saveAll(request.getPersons());
                insertedCounts.put("persons", savedPersons.size());
                log.info("Successfully inserted {} persons", savedPersons.size());
            }
            
            // Insert Criminal Records
            if (request.getCriminalRecords() != null && !request.getCriminalRecords().isEmpty()) {
                log.info("Inserting {} criminal records", request.getCriminalRecords().size());
                List<CriminalRecord> savedRecords = criminalRecordRepository.saveAll(request.getCriminalRecords());
                insertedCounts.put("criminalRecords", savedRecords.size());
                log.info("Successfully inserted {} criminal records", savedRecords.size());
            }
            
            // Insert Bank Records
            if (request.getBankRecords() != null && !request.getBankRecords().isEmpty()) {
                log.info("Inserting {} bank records", request.getBankRecords().size());
                List<BankRecord> savedRecords = bankRecordRepository.saveAll(request.getBankRecords());
                insertedCounts.put("bankRecords", savedRecords.size());
                log.info("Successfully inserted {} bank records", savedRecords.size());
            }
            
            // Insert Loan Records
            if (request.getLoans() != null && !request.getLoans().isEmpty()) {
                log.info("Inserting {} loan records", request.getLoans().size());
                List<HistoricalAndCurrentLoan> savedLoans = loanRepository.saveAll(request.getLoans());
                insertedCounts.put("loans", savedLoans.size());
                log.info("Successfully inserted {} loan records", savedLoans.size());
            }
            
            // Insert Government Documents
            if (request.getGovernmentDocuments() != null && !request.getGovernmentDocuments().isEmpty()) {
                log.info("Inserting {} government documents", request.getGovernmentDocuments().size());
                List<GovernmentIssuedDocument> savedDocs = documentRepository.saveAll(request.getGovernmentDocuments());
                insertedCounts.put("governmentDocuments", savedDocs.size());
                log.info("Successfully inserted {} government documents", savedDocs.size());
            }
            
            result.setSuccess(true);
            result.setMessage("Bulk data insertion completed successfully");
            result.setInsertedCounts(insertedCounts);
            
            int totalInserted = insertedCounts.values().stream().mapToInt(Integer::intValue).sum();
            log.info("Bulk insertion completed successfully. Total records inserted: {}", totalInserted);
            
        } catch (Exception e) {
            log.error("Error during bulk data insertion", e);
            result.setSuccess(false);
            result.setMessage("Bulk data insertion failed: " + e.getMessage());
            result.setInsertedCounts(insertedCounts);
            errors.put("general", e.getMessage());
            result.setErrors(errors);
        }
        
        return result;
    }
    
    @Transactional
    public void clearAllData() {
        log.info("Clearing all external fraud database data");
        
        // Delete in reverse order to maintain referential integrity
        criminalRecordRepository.deleteAll();
        bankRecordRepository.deleteAll();
        loanRepository.deleteAll();
        documentRepository.deleteAll();
        personRepository.deleteAll();
        
        log.info("All external fraud database data cleared successfully");
    }
    
    public DatabaseStats getDatabaseStats() {
        DatabaseStats stats = new DatabaseStats();
        
        stats.setPersonsCount(personRepository.count());
        stats.setCriminalRecordsCount(criminalRecordRepository.count());
        stats.setBankRecordsCount(bankRecordRepository.count());
        stats.setLoansCount(loanRepository.count());
        stats.setGovernmentDocumentsCount(documentRepository.count());
        stats.setTotalRecords(stats.getPersonsCount() + stats.getCriminalRecordsCount() + 
                             stats.getBankRecordsCount() + stats.getLoansCount() + 
                             stats.getGovernmentDocumentsCount());
        
        return stats;
    }
    
    // Inner classes for request/response
    public static class BulkDataRequest {
        private List<Person> persons;
        private List<CriminalRecord> criminalRecords;
        private List<BankRecord> bankRecords;
        private List<HistoricalAndCurrentLoan> loans;
        private List<GovernmentIssuedDocument> governmentDocuments;
        
        // Getters and setters
        public List<Person> getPersons() { return persons; }
        public void setPersons(List<Person> persons) { this.persons = persons; }
        
        public List<CriminalRecord> getCriminalRecords() { return criminalRecords; }
        public void setCriminalRecords(List<CriminalRecord> criminalRecords) { this.criminalRecords = criminalRecords; }
        
        public List<BankRecord> getBankRecords() { return bankRecords; }
        public void setBankRecords(List<BankRecord> bankRecords) { this.bankRecords = bankRecords; }
        
        public List<HistoricalAndCurrentLoan> getLoans() { return loans; }
        public void setLoans(List<HistoricalAndCurrentLoan> loans) { this.loans = loans; }
        
        public List<GovernmentIssuedDocument> getGovernmentDocuments() { return governmentDocuments; }
        public void setGovernmentDocuments(List<GovernmentIssuedDocument> governmentDocuments) { this.governmentDocuments = governmentDocuments; }
    }
    
    public static class BulkInsertionResult {
        private boolean success;
        private String message;
        private Map<String, Integer> insertedCounts;
        private Map<String, String> errors;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Map<String, Integer> getInsertedCounts() { return insertedCounts; }
        public void setInsertedCounts(Map<String, Integer> insertedCounts) { this.insertedCounts = insertedCounts; }
        
        public Map<String, String> getErrors() { return errors; }
        public void setErrors(Map<String, String> errors) { this.errors = errors; }
    }
    
    public static class DatabaseStats {
        private long personsCount;
        private long criminalRecordsCount;
        private long bankRecordsCount;
        private long loansCount;
        private long governmentDocumentsCount;
        private long totalRecords;
        
        // Getters and setters
        public long getPersonsCount() { return personsCount; }
        public void setPersonsCount(long personsCount) { this.personsCount = personsCount; }
        
        public long getCriminalRecordsCount() { return criminalRecordsCount; }
        public void setCriminalRecordsCount(long criminalRecordsCount) { this.criminalRecordsCount = criminalRecordsCount; }
        
        public long getBankRecordsCount() { return bankRecordsCount; }
        public void setBankRecordsCount(long bankRecordsCount) { this.bankRecordsCount = bankRecordsCount; }
        
        public long getLoansCount() { return loansCount; }
        public void setLoansCount(long loansCount) { this.loansCount = loansCount; }
        
        public long getGovernmentDocumentsCount() { return governmentDocumentsCount; }
        public void setGovernmentDocumentsCount(long governmentDocumentsCount) { this.governmentDocumentsCount = governmentDocumentsCount; }
        
        public long getTotalRecords() { return totalRecords; }
        public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
    }
}
