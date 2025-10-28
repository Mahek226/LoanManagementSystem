package com.tss.springsecurity.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import com.tss.springsecurity.dto.LoanApplicationDTO;
import com.tss.springsecurity.dto.SimpleLoanApplicationDTO;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loan-applications")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class LoanApplicationController {
    
    private final LoanApplicationService loanApplicationService;
    private final com.tss.springsecurity.repository.ApplicantLoanDetailsRepository loanDetailsRepository;
    
    public LoanApplicationController(LoanApplicationService loanApplicationService,
                                    com.tss.springsecurity.repository.ApplicantLoanDetailsRepository loanDetailsRepository) {
        this.loanApplicationService = loanApplicationService;
        this.loanDetailsRepository = loanDetailsRepository;
    }
    
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitLoanApplication(
            @Valid @RequestBody LoanApplicationDTO loanApplicationDTO) {
        try {
            Applicant applicant = loanApplicationService.submitLoanApplication(loanApplicationDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan details captured successfully, proceed with document uploading");
            response.put("applicantId", applicant.getApplicantId());
            response.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());
            response.put("email", applicant.getEmail());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Applicant> getApplicant(@PathVariable Long applicantId) {
        try {
            Applicant applicant = loanApplicationService.getApplicantById(applicantId);
            return new ResponseEntity<>(applicant, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/applicant/{applicantId}/loans")
    public ResponseEntity<List<ApplicantLoanDetails>> getApplicantLoans(@PathVariable Long applicantId) {
        List<ApplicantLoanDetails> loans = loanApplicationService.getApplicantLoans(applicantId);
        return new ResponseEntity<>(loans, HttpStatus.OK);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<Applicant>> getAllApplicants() {
        List<Applicant> applicants = loanApplicationService.getAllApplicants();
        return new ResponseEntity<>(applicants, HttpStatus.OK);
    }
    
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<ApplicantLoanDetails> getLoanDetails(@PathVariable Long loanId) {
        try {
            ApplicantLoanDetails loan = loanApplicationService.getLoanById(loanId);
            return new ResponseEntity<>(loan, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PostMapping("/submit-complete-bulk")
    public ResponseEntity<Map<String, Object>> submitBulkLoanApplications(
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> successfulApplications = new ArrayList<>();
        List<Map<String, Object>> failedApplications = new ArrayList<>();
        
        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // Validate file type - accept JSON and JSONL files
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            
            // Accept application/json, application/x-ndjson, text/plain, or files with .json/.jsonl extension
            boolean isValidType = (contentType != null && 
                                  (contentType.equals("application/json") || 
                                   contentType.equals("application/x-ndjson") ||
                                   contentType.equals("text/plain") ||
                                   contentType.startsWith("text/"))) ||
                                  (originalFilename != null && 
                                  (originalFilename.endsWith(".json") || 
                                   originalFilename.endsWith(".jsonl")));
            
            if (!isValidType) {
                response.put("success", false);
                response.put("message", "Only JSON/JSONL files are accepted. Received content type: " + contentType);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // Parse JSON file - supports both array format and JSONL (one object per line)
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules(); // Register JavaTimeModule for LocalDate
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            List<CompleteLoanApplicationDTO> applications = new ArrayList<>();
            
            try {
                // First, try to parse as JSON array
                applications = objectMapper.readValue(
                    file.getInputStream(), 
                    new TypeReference<List<CompleteLoanApplicationDTO>>() {}
                );
            } catch (Exception e) {
                // If array parsing fails, try JSONL format (one JSON object per line)
                try {
                    String content = new String(file.getBytes());
                    String[] lines = content.split("\\r?\\n");
                    
                    for (String line : lines) {
                        if (line != null && !line.trim().isEmpty()) {
                            CompleteLoanApplicationDTO dto = objectMapper.readValue(
                                line, 
                                CompleteLoanApplicationDTO.class
                            );
                            applications.add(dto);
                        }
                    }
                } catch (Exception jsonlException) {
                    response.put("success", false);
                    response.put("message", "Invalid JSON format. Expected either JSON array or JSONL (one object per line): " + jsonlException.getMessage());
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            
            if (applications.isEmpty()) {
                response.put("success", false);
                response.put("message", "No applications found in the file");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // Process each application
            int recordNumber = 0;
            for (CompleteLoanApplicationDTO dto : applications) {
                recordNumber++;
                try {
                    Applicant applicant = loanApplicationService.submitCompleteLoanApplication(dto);
                    
                    Map<String, Object> successRecord = new HashMap<>();
                    successRecord.put("recordNumber", recordNumber);
                    successRecord.put("applicantId", applicant.getApplicantId());
                    successRecord.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());
                    successRecord.put("email", applicant.getEmail());
                    successRecord.put("phone", applicant.getPhone());
                    successfulApplications.add(successRecord);
                    
                } catch (Exception e) {
                    Map<String, Object> failedRecord = new HashMap<>();
                    failedRecord.put("recordNumber", recordNumber);
                    failedRecord.put("email", dto.getApplicant() != null ? dto.getApplicant().getEmail() : "N/A");
                    failedRecord.put("error", e.getMessage());
                    failedApplications.add(failedRecord);
                }
            }
            
            // Build response
            response.put("success", true);
            response.put("message", "Bulk processing completed");
            response.put("totalRecords", applications.size());
            response.put("successCount", successfulApplications.size());
            response.put("failureCount", failedApplications.size());
            response.put("successfulApplications", successfulApplications);
            response.put("failedApplications", failedApplications);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing bulk upload: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/submit-complete")
    public ResponseEntity<Map<String, Object>> submitSimpleLoanApplication(
            @Valid @RequestBody SimpleLoanApplicationDTO applicationDTO) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get applicant
            Applicant applicant = loanApplicationService.getApplicantById(applicationDTO.getApplicantId());
            
            if (applicant == null) {
                response.put("success", false);
                response.put("message", "Applicant not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            
            // Create loan details
            ApplicantLoanDetails loanDetails = new ApplicantLoanDetails();
            loanDetails.setApplicant(applicant);
            
            // Set basic details
            SimpleLoanApplicationDTO.BasicDetails basic = applicationDTO.getBasicDetails();
            loanDetails.setLoanType(basic.getLoanType());
            loanDetails.setLoanAmount(basic.getLoanAmount());
            loanDetails.setTenureMonths(basic.getTenure());
            loanDetails.setLoanPurpose(basic.getPurpose());
            
            // Set financial details
            SimpleLoanApplicationDTO.FinancialDetails financial = applicationDTO.getFinancialDetails();
            loanDetails.setEmploymentType(financial.getEmploymentType());
            loanDetails.setEmployerName(financial.getEmployerName());
            loanDetails.setDesignation(financial.getDesignation());
            loanDetails.setMonthlyIncome(financial.getMonthlyGrossSalary() != null ? 
                financial.getMonthlyGrossSalary() : financial.getMonthlyNetSalary());
            
            // Set bank details
            loanDetails.setBankName(financial.getBankName());
            loanDetails.setAccountNumber(financial.getAccountNumber());
            loanDetails.setIfscCode(financial.getIfscCode());
            loanDetails.setAccountType(financial.getAccountType());
            
            // Set obligations
            BigDecimal totalObligations = BigDecimal.ZERO;
            if (financial.getExistingLoanEMI() != null) {
                totalObligations = totalObligations.add(financial.getExistingLoanEMI());
            }
            if (financial.getCreditCardPayment() != null) {
                totalObligations = totalObligations.add(financial.getCreditCardPayment());
            }
            if (financial.getOtherObligations() != null) {
                totalObligations = totalObligations.add(financial.getOtherObligations());
            }
            loanDetails.setExistingObligations(totalObligations);
            
            // Set co-applicant details if present
            if (basic.getHasCoApplicant() != null && basic.getHasCoApplicant()) {
                loanDetails.setHasCoApplicant(true);
                loanDetails.setCoApplicantName(basic.getCoApplicantName());
                loanDetails.setCoApplicantRelation(basic.getCoApplicantRelation());
            }
            
            // Set collateral details if present
            if (basic.getHasCollateral() != null && basic.getHasCollateral()) {
                loanDetails.setHasCollateral(true);
                loanDetails.setCollateralType(basic.getCollateralType());
                loanDetails.setCollateralValue(basic.getCollateralValue());
            }
            
            // Set status
            loanDetails.setApplicationStatus("SUBMITTED");
            loanDetails.setLoanStatus("PENDING");
            
            // Save loan application
            ApplicantLoanDetails savedLoan = loanDetailsRepository.save(loanDetails);
            
            // Prepare response
            response.put("success", true);
            response.put("message", "Loan application submitted successfully!");
            response.put("loanId", savedLoan.getLoanId());
            response.put("applicantId", applicant.getApplicantId());
            response.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());
            response.put("loanType", savedLoan.getLoanType());
            response.put("loanAmount", savedLoan.getLoanAmount());
            response.put("status", savedLoan.getApplicationStatus());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to submit application: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
