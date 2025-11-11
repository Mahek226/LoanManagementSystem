package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.CompleteLoanApplicationRequest;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.service.impl.ComprehensiveLoanApplicationServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
//@RestController  // Disabled until Cloudinary dependency is added
@RequestMapping("/api/simple-loan")
public class SimpleLoanApplicationController {

    @Autowired
    private ComprehensiveLoanApplicationServiceImpl loanApplicationService;

    @PostMapping("/submit/{username}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> submitLoanApplication(
            @PathVariable String username,
            @Valid @RequestBody CompleteLoanApplicationRequest request) {
        
        try {
            // Submit the application without documents first
            Applicant result = loanApplicationService.submitCompleteLoanApplication(request, username);
            
            return ResponseEntity.ok(new MessageResponse(
                "Loan application submitted successfully! Application ID: " + result.getApplicantId()));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing application: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Error submitting loan application: " + e.getMessage()));
        }
    }

    @PostMapping("/submit-with-basic-info")
    public ResponseEntity<?> submitBasicLoanApplication(@RequestBody BasicLoanRequest request) {
        try {
            // Create a simplified request
            CompleteLoanApplicationRequest fullRequest = new CompleteLoanApplicationRequest();
            
            // Set basic required fields
            fullRequest.setLoanType(request.getLoanType());
            fullRequest.setFirstName(request.getFirstName());
            fullRequest.setLastName(request.getLastName());
            fullRequest.setDateOfBirth(request.getDateOfBirth());
            fullRequest.setGender(request.getGender());
            fullRequest.setPhone(request.getPhone());
            fullRequest.setEmail(request.getEmail());
            fullRequest.setCurrentAddress(request.getCurrentAddress());
            fullRequest.setCurrentCity(request.getCurrentCity());
            fullRequest.setCurrentState(request.getCurrentState());
            fullRequest.setCurrentPincode(request.getCurrentPincode());
            fullRequest.setLoanAmount(request.getLoanAmount());
            fullRequest.setLoanTenure(request.getLoanTenure());
            fullRequest.setLoanPurpose(request.getLoanPurpose());
            fullRequest.setEmploymentType(request.getEmploymentType());
            fullRequest.setMonthlyIncome(request.getMonthlyIncome());
            fullRequest.setBankName(request.getBankName());
            fullRequest.setAccountNumber(request.getAccountNumber());
            fullRequest.setIfscCode(request.getIfscCode());
            fullRequest.setAccountType(request.getAccountType());
            fullRequest.setAgreeToTerms(true);
            fullRequest.setSameAsCurrent(true);
            
            Applicant result = loanApplicationService.submitCompleteLoanApplication(fullRequest, request.getUsername());
            
            return ResponseEntity.ok(new MessageResponse(
                "Basic loan application submitted successfully! Application ID: " + result.getApplicantId()));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Error submitting loan application: " + e.getMessage()));
        }
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<List<Applicant>> getAllApplications() {
        try {
            List<Applicant> applications = loanApplicationService.getAllLoanApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/application/{username}")
    public ResponseEntity<?> getApplicationByUsername(@PathVariable String username) {
        try {
            Applicant application = loanApplicationService.getLoanApplicationByUsername(username);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    // Inner class for basic loan request
    public static class BasicLoanRequest {
        private String username;
        private String loanType;
        private String firstName;
        private String lastName;
        private java.time.LocalDate dateOfBirth;
        private String gender;
        private String phone;
        private String email;
        private String currentAddress;
        private String currentCity;
        private String currentState;
        private String currentPincode;
        private java.math.BigDecimal loanAmount;
        private Integer loanTenure;
        private String loanPurpose;
        private String employmentType;
        private java.math.BigDecimal monthlyIncome;
        private String bankName;
        private String accountNumber;
        private String ifscCode;
        private String accountType;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getLoanType() { return loanType; }
        public void setLoanType(String loanType) { this.loanType = loanType; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCurrentAddress() { return currentAddress; }
        public void setCurrentAddress(String currentAddress) { this.currentAddress = currentAddress; }
        public String getCurrentCity() { return currentCity; }
        public void setCurrentCity(String currentCity) { this.currentCity = currentCity; }
        public String getCurrentState() { return currentState; }
        public void setCurrentState(String currentState) { this.currentState = currentState; }
        public String getCurrentPincode() { return currentPincode; }
        public void setCurrentPincode(String currentPincode) { this.currentPincode = currentPincode; }
        public java.math.BigDecimal getLoanAmount() { return loanAmount; }
        public void setLoanAmount(java.math.BigDecimal loanAmount) { this.loanAmount = loanAmount; }
        public Integer getLoanTenure() { return loanTenure; }
        public void setLoanTenure(Integer loanTenure) { this.loanTenure = loanTenure; }
        public String getLoanPurpose() { return loanPurpose; }
        public void setLoanPurpose(String loanPurpose) { this.loanPurpose = loanPurpose; }
        public String getEmploymentType() { return employmentType; }
        public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
        public java.math.BigDecimal getMonthlyIncome() { return monthlyIncome; }
        public void setMonthlyIncome(java.math.BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getIfscCode() { return ifscCode; }
        public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
    }
}
