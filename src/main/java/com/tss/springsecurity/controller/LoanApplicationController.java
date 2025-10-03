package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import com.tss.springsecurity.dto.LoanApplicationDTO;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {
    
    private final LoanApplicationService loanApplicationService;
    
    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }
    
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitLoanApplication(
            @Valid @RequestBody LoanApplicationDTO loanApplicationDTO) {
        try {
            Applicant applicant = loanApplicationService.submitLoanApplication(loanApplicationDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan application submitted successfully");
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
    
    @PostMapping("/submit-complete")
    public ResponseEntity<Map<String, Object>> submitCompleteLoanApplication(
            @Valid @RequestBody CompleteLoanApplicationDTO completeLoanApplicationDTO) {
        try {
            Applicant applicant = loanApplicationService.submitCompleteLoanApplication(completeLoanApplicationDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Complete loan application submitted successfully with all details");
            response.put("applicantId", applicant.getApplicantId());
            response.put("applicantName", applicant.getFirstName() + " " + applicant.getLastName());
            response.put("email", applicant.getEmail());
            response.put("phone", applicant.getPhone());
            
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
}
