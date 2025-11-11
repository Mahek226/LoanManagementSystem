package com.tss.springsecurity.externalfraud.engine;

import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckRequest;
import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckResult;
import com.tss.springsecurity.externalfraud.model.ExternalFraudFlag;
import com.tss.springsecurity.externalfraud.entity.*;
import com.tss.springsecurity.externalfraud.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ExternalFraudRuleEngine {
    
    @Autowired
    private PersonRepository personRepository;
    
    @Autowired
    private CriminalRecordRepository criminalRecordRepository;
    
    @Autowired
    private HistoricalAndCurrentLoanRepository loanRepository;
    
    @Autowired
    private BankRecordRepository bankRecordRepository;
    
    @Autowired
    private GovernmentIssuedDocumentRepository documentRepository;
    
    public ExternalFraudCheckResult performFraudCheck(ExternalFraudCheckRequest request) {
        long startTime = System.currentTimeMillis();
        
        ExternalFraudCheckResult result = ExternalFraudCheckResult.builder()
                .screeningTimestamp(LocalDateTime.now())
                .screeningVersion("1.0")
                .build();
        
        try {
            // Step 1: Find person in external database
            Person person = findPersonInExternalDB(request);
            
            if (person == null) {
                result.setPersonFound(false);
                result.setRiskLevel("CLEAN");
                result.setRecommendation("APPROVE");
                log.info("Person not found in external database for PAN: {}", maskPan(request.getPanNumber()));
                return result;
            }
            
            result.setPersonFound(true);
            result.setExternalPersonId(person.getId());
            
            // Step 2: Run fraud rules
            if (request.isCheckCriminalRecords()) {
                checkCriminalRecords(person.getId(), result);
            }
            
            if (request.isCheckLoanHistory()) {
                checkLoanHistory(person.getId(), result);
            }
            
            if (request.isCheckBankRecords()) {
                checkBankRecords(person.getId(), result);
            }
            
            if (request.isCheckDocumentVerification()) {
                checkDocumentVerification(person.getId(), result);
            }
            
            // Step 3: Calculate final risk assessment
            result.calculateRiskLevel();
            
            log.info("External fraud check completed for person ID: {} with risk level: {}", 
                    person.getId(), result.getRiskLevel());
            
        } catch (Exception e) {
            log.error("Error during external fraud check", e);
            result.addFraudFlag(ExternalFraudFlag.create(
                "EXTERNAL_SYSTEM_ERROR",
                "External System Error",
                "SYSTEM",
                "MEDIUM",
                25,
                "Error occurred during external fraud screening",
                e.getMessage()
            ));
            result.calculateRiskLevel();
        } finally {
            result.setScreeningDurationMs(System.currentTimeMillis() - startTime);
        }
        
        return result;
    }
    
    private Person findPersonInExternalDB(ExternalFraudCheckRequest request) {
        // Try to find by PAN first
        if (request.getPanNumber() != null) {
            Optional<Person> personByPan = personRepository.findByPanNumber(request.getPanNumber());
            if (personByPan.isPresent()) {
                return personByPan.get();
            }
        }
        
        // Try to find by Aadhaar
        if (request.getAadhaarNumber() != null) {
            Optional<Person> personByAadhaar = personRepository.findByAadhaarNumber(request.getAadhaarNumber());
            if (personByAadhaar.isPresent()) {
                return personByAadhaar.get();
            }
        }
        
        // Try to find by phone
        if (request.getPhoneNumber() != null) {
            List<Person> personsByPhone = personRepository.findByPhoneNumber(request.getPhoneNumber());
            if (!personsByPhone.isEmpty()) {
                return personsByPhone.get(0); // Return first match
            }
        }
        
        // Try to find by email
        if (request.getEmail() != null) {
            List<Person> personsByEmail = personRepository.findByEmail(request.getEmail());
            if (!personsByEmail.isEmpty()) {
                return personsByEmail.get(0); // Return first match
            }
        }
        
        return null;
    }
    
    private void checkCriminalRecords(Long personId, ExternalFraudCheckResult result) {
        List<CriminalRecord> criminalRecords = criminalRecordRepository.findByPersonId(personId);
        
        result.setHasCriminalRecord(!criminalRecords.isEmpty());
        result.setTotalCriminalCases(criminalRecords.size());
        
        if (!criminalRecords.isEmpty()) {
            long convictedCases = criminalRecordRepository.countConvictedCases(personId);
            long openCases = criminalRecordRepository.countOpenCases(personId);
            
            result.setConvictedCases(convictedCases);
            result.setOpenCases(openCases);
            
            // Extract case types
            criminalRecords.forEach(record -> {
                if (!result.getCriminalCaseTypes().contains(record.getCaseType())) {
                    result.getCriminalCaseTypes().add(record.getCaseType());
                }
            });
            
            // Apply fraud rules for criminal records
            if (convictedCases > 0) {
                result.addFraudFlag(ExternalFraudFlag.create(
                    "CRIMINAL_CONVICTION",
                    "Criminal Conviction Found",
                    "CRIMINAL",
                    "CRITICAL",
                    100,
                    "Person has " + convictedCases + " criminal conviction(s)",
                    "Convicted cases: " + convictedCases + ", Case types: " + String.join(", ", result.getCriminalCaseTypes())
                ));
            }
            
            if (openCases > 0) {
                result.addFraudFlag(ExternalFraudFlag.create(
                    "CRIMINAL_OPEN_CASE",
                    "Open Criminal Case",
                    "CRIMINAL",
                    "HIGH",
                    60,
                    "Person has " + openCases + " open criminal case(s)",
                    "Open cases: " + openCases + ", Case types: " + String.join(", ", result.getCriminalCaseTypes())
                ));
            }
        }
    }
    
    private void checkLoanHistory(Long personId, ExternalFraudCheckResult result) {
        List<HistoricalAndCurrentLoan> loans = loanRepository.findByPersonId(personId);
        
        result.setHasLoanHistory(!loans.isEmpty());
        result.setTotalLoans(loans.size());
        
        if (!loans.isEmpty()) {
            long activeLoans = loanRepository.countActiveLoans(personId);
            long defaultedLoans = loanRepository.countDefaultedLoans(personId);
            BigDecimal totalOutstanding = loanRepository.getTotalOutstandingBalance(personId);
            
            result.setActiveLoans(activeLoans);
            result.setDefaultedLoans(defaultedLoans);
            result.setTotalOutstandingAmount(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO);
            
            // Find worst loan status
            String worstStatus = "CLOSED";
            for (HistoricalAndCurrentLoan loan : loans) {
                if ("DEFAULTED".equals(loan.getStatus())) {
                    worstStatus = "DEFAULTED";
                    break;
                } else if ("ACTIVE".equals(loan.getStatus()) && !"DEFAULTED".equals(worstStatus)) {
                    worstStatus = "ACTIVE";
                }
            }
            result.setWorstLoanStatus(worstStatus);
            
            // Apply fraud rules for loan history
            if (defaultedLoans > 0) {
                int points = defaultedLoans >= 3 ? 80 : (defaultedLoans >= 2 ? 60 : 40);
                String severity = defaultedLoans >= 3 ? "CRITICAL" : (defaultedLoans >= 2 ? "HIGH" : "MEDIUM");
                
                result.addFraudFlag(ExternalFraudFlag.create(
                    "LOAN_DEFAULT_HISTORY",
                    "Loan Default History",
                    "LOAN_HISTORY",
                    severity,
                    points,
                    "Person has " + defaultedLoans + " defaulted loan(s)",
                    "Total defaulted loans: " + defaultedLoans + ", Total outstanding: ₹" + result.getTotalOutstandingAmount()
                ));
            }
            
            if (activeLoans >= 5) {
                result.addFraudFlag(ExternalFraudFlag.create(
                    "MULTIPLE_ACTIVE_LOANS",
                    "Multiple Active Loans",
                    "LOAN_HISTORY",
                    "HIGH",
                    50,
                    "Person has " + activeLoans + " active loans",
                    "Active loans may indicate over-leveraging"
                ));
            }
            
            if (totalOutstanding != null && totalOutstanding.compareTo(new BigDecimal("1000000")) > 0) {
                result.addFraudFlag(ExternalFraudFlag.create(
                    "HIGH_OUTSTANDING_DEBT",
                    "High Outstanding Debt",
                    "LOAN_HISTORY",
                    "HIGH",
                    45,
                    "High total outstanding debt of ₹" + totalOutstanding,
                    "May indicate financial stress"
                ));
            }
        }
    }
    
    private void checkBankRecords(Long personId, ExternalFraudCheckResult result) {
        List<BankRecord> bankRecords = bankRecordRepository.findByPersonId(personId);
        
        result.setHasBankRecords(!bankRecords.isEmpty());
        result.setTotalBankAccounts(bankRecords.size());
        
        if (!bankRecords.isEmpty()) {
            BigDecimal totalBalance = bankRecords.stream()
                    .filter(record -> record.getBalanceAmount() != null)
                    .map(BankRecord::getBalanceAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            result.setTotalBankBalance(totalBalance);
            
            // Check for suspicious bank patterns
            long inactiveAccounts = bankRecords.stream()
                    .mapToLong(record -> record.getIsActive() ? 0 : 1)
                    .sum();
            
            if (bankRecords.size() >= 10) {
                result.addFraudFlag(ExternalFraudFlag.create(
                    "EXCESSIVE_BANK_ACCOUNTS",
                    "Excessive Bank Accounts",
                    "BANK_RECORDS",
                    "MEDIUM",
                    30,
                    "Person has " + bankRecords.size() + " bank accounts",
                    "May indicate money laundering or fraud"
                ));
            }
            
            if (inactiveAccounts >= 5) {
                result.addFraudFlag(ExternalFraudFlag.create(
                    "MULTIPLE_INACTIVE_ACCOUNTS",
                    "Multiple Inactive Accounts",
                    "BANK_RECORDS",
                    "MEDIUM",
                    25,
                    "Person has " + inactiveAccounts + " inactive bank accounts",
                    "Suspicious account opening pattern"
                ));
            }
        }
    }
    
    private void checkDocumentVerification(Long personId, ExternalFraudCheckResult result) {
        List<GovernmentIssuedDocument> documents = documentRepository.findByPersonId(personId);
        
        if (!documents.isEmpty()) {
            for (GovernmentIssuedDocument doc : documents) {
                if ("EXPIRED".equals(doc.getVerificationStatus())) {
                    result.setHasDocumentIssues(true);
                    result.getDocumentIssues().add(doc.getDocumentType() + " expired");
                    
                    result.addFraudFlag(ExternalFraudFlag.create(
                        "EXPIRED_DOCUMENT",
                        "Expired Document",
                        "DOCUMENT_VERIFICATION",
                        "MEDIUM",
                        20,
                        doc.getDocumentType() + " document has expired",
                        "Document: " + doc.getDocumentType() + ", Expiry: " + doc.getExpiryDate()
                    ));
                }
                
                if ("UNVERIFIED".equals(doc.getVerificationStatus())) {
                    result.setHasDocumentIssues(true);
                    result.getDocumentIssues().add(doc.getDocumentType() + " unverified");
                    
                    result.addFraudFlag(ExternalFraudFlag.create(
                        "UNVERIFIED_DOCUMENT",
                        "Unverified Document",
                        "DOCUMENT_VERIFICATION",
                        "MEDIUM",
                        15,
                        doc.getDocumentType() + " document is unverified",
                        "Document verification pending or failed"
                    ));
                }
            }
        }
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        return "****" + pan.substring(pan.length() - 4);
    }
}
