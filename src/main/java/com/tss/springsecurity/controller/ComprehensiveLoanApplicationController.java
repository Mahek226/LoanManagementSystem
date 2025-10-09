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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
//@RestController  // Disabled until Cloudinary dependency is added
@RequestMapping("/api/loan-application")
public class ComprehensiveLoanApplicationController {

    @Autowired
    private ComprehensiveLoanApplicationServiceImpl loanApplicationService;

    @PostMapping("/submit")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> submitLoanApplication(
            @RequestParam("loanType") String loanType,
            @RequestParam("firstName") String firstName,
            @RequestParam(value = "middleName", required = false) String middleName,
            @RequestParam("lastName") String lastName,
            @RequestParam("dateOfBirth") String dateOfBirth,
            @RequestParam("gender") String gender,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam("currentAddress") String currentAddress,
            @RequestParam("currentCity") String currentCity,
            @RequestParam("currentState") String currentState,
            @RequestParam("currentPincode") String currentPincode,
            @RequestParam(value = "sameAsCurrent", defaultValue = "false") Boolean sameAsCurrent,
            @RequestParam(value = "permanentAddress", required = false) String permanentAddress,
            @RequestParam(value = "permanentCity", required = false) String permanentCity,
            @RequestParam(value = "permanentState", required = false) String permanentState,
            @RequestParam(value = "permanentPincode", required = false) String permanentPincode,
            @RequestParam("loanAmount") String loanAmount,
            @RequestParam("loanTenure") Integer loanTenure,
            @RequestParam("loanPurpose") String loanPurpose,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "propertyAddress", required = false) String propertyAddress,
            @RequestParam(value = "propertyCity", required = false) String propertyCity,
            @RequestParam(value = "propertyState", required = false) String propertyState,
            @RequestParam(value = "propertyPincode", required = false) String propertyPincode,
            @RequestParam(value = "propertyValue", required = false) String propertyValue,
            @RequestParam(value = "constructionStatus", required = false) String constructionStatus,
            @RequestParam("employmentType") String employmentType,
            @RequestParam(value = "companyName", required = false) String companyName,
            @RequestParam(value = "designation", required = false) String designation,
            @RequestParam(value = "workExperience", required = false) Integer workExperience,
            @RequestParam(value = "officeAddress", required = false) String officeAddress,
            @RequestParam(value = "officeCity", required = false) String officeCity,
            @RequestParam(value = "officeState", required = false) String officeState,
            @RequestParam(value = "officePincode", required = false) String officePincode,
            @RequestParam("monthlyIncome") String monthlyIncome,
            @RequestParam(value = "otherIncome", required = false) String otherIncome,
            @RequestParam(value = "monthlyExpenses", required = false) String monthlyExpenses,
            @RequestParam(value = "existingLoanEmi", required = false) String existingLoanEmi,
            @RequestParam(value = "creditCardOutstanding", required = false) String creditCardOutstanding,
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("ifscCode") String ifscCode,
            @RequestParam("accountType") String accountType,
            @RequestParam(value = "hasCoApplicant", defaultValue = "false") Boolean hasCoApplicant,
            @RequestParam(value = "coApplicantFirstName", required = false) String coApplicantFirstName,
            @RequestParam(value = "coApplicantLastName", required = false) String coApplicantLastName,
            @RequestParam(value = "coApplicantDateOfBirth", required = false) String coApplicantDateOfBirth,
            @RequestParam(value = "coApplicantPhone", required = false) String coApplicantPhone,
            @RequestParam(value = "coApplicantEmail", required = false) String coApplicantEmail,
            @RequestParam(value = "coApplicantRelation", required = false) String coApplicantRelation,
            @RequestParam(value = "coApplicantIncome", required = false) String coApplicantIncome,
            @RequestParam(value = "aadhaarNumber", required = false) String aadhaarNumber,
            @RequestParam(value = "panNumber", required = false) String panNumber,
            @RequestParam(value = "passportNumber", required = false) String passportNumber,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "agreeToTerms", defaultValue = "false") Boolean agreeToTerms,
            @RequestParam(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentTypes", required = false) List<String> documentTypes,
            @RequestParam("applicantUsername") String applicantUsername) {

        try {
            // Create request object from parameters
            CompleteLoanApplicationRequest request = new CompleteLoanApplicationRequest();
            
            // Set all the fields
            request.setLoanType(loanType);
            request.setFirstName(firstName);
            request.setMiddleName(middleName);
            request.setLastName(lastName);
            request.setDateOfBirth(LocalDate.parse(dateOfBirth));
            request.setGender(gender);
            request.setPhone(phone);
            request.setEmail(email);
            request.setCurrentAddress(currentAddress);
            request.setCurrentCity(currentCity);
            request.setCurrentState(currentState);
            request.setCurrentPincode(currentPincode);
            request.setSameAsCurrent(sameAsCurrent);
            request.setPermanentAddress(permanentAddress);
            request.setPermanentCity(permanentCity);
            request.setPermanentState(permanentState);
            request.setPermanentPincode(permanentPincode);
            request.setLoanAmount(new BigDecimal(loanAmount));
            request.setLoanTenure(loanTenure);
            request.setLoanPurpose(loanPurpose);
            request.setPropertyType(propertyType);
            request.setPropertyAddress(propertyAddress);
            request.setPropertyCity(propertyCity);
            request.setPropertyState(propertyState);
            request.setPropertyPincode(propertyPincode);
            if (propertyValue != null && !propertyValue.isEmpty()) {
                request.setPropertyValue(new BigDecimal(propertyValue));
            }
            request.setConstructionStatus(constructionStatus);
            request.setEmploymentType(employmentType);
            request.setCompanyName(companyName);
            request.setDesignation(designation);
            request.setWorkExperience(workExperience);
            request.setOfficeAddress(officeAddress);
            request.setOfficeCity(officeCity);
            request.setOfficeState(officeState);
            request.setOfficePincode(officePincode);
            request.setMonthlyIncome(new BigDecimal(monthlyIncome));
            if (otherIncome != null && !otherIncome.isEmpty()) {
                request.setOtherIncome(new BigDecimal(otherIncome));
            }
            if (monthlyExpenses != null && !monthlyExpenses.isEmpty()) {
                request.setMonthlyExpenses(new BigDecimal(monthlyExpenses));
            }
            if (existingLoanEmi != null && !existingLoanEmi.isEmpty()) {
                request.setExistingLoanEmi(new BigDecimal(existingLoanEmi));
            }
            if (creditCardOutstanding != null && !creditCardOutstanding.isEmpty()) {
                request.setCreditCardOutstanding(new BigDecimal(creditCardOutstanding));
            }
            request.setBankName(bankName);
            request.setAccountNumber(accountNumber);
            request.setIfscCode(ifscCode);
            request.setAccountType(accountType);
            request.setHasCoApplicant(hasCoApplicant);
            request.setCoApplicantFirstName(coApplicantFirstName);
            request.setCoApplicantLastName(coApplicantLastName);
            if (coApplicantDateOfBirth != null && !coApplicantDateOfBirth.isEmpty()) {
                request.setCoApplicantDateOfBirth(LocalDate.parse(coApplicantDateOfBirth));
            }
            request.setCoApplicantPhone(coApplicantPhone);
            request.setCoApplicantEmail(coApplicantEmail);
            request.setCoApplicantRelation(coApplicantRelation);
            if (coApplicantIncome != null && !coApplicantIncome.isEmpty()) {
                request.setCoApplicantIncome(new BigDecimal(coApplicantIncome));
            }
            request.setAadhaarNumber(aadhaarNumber);
            request.setPanNumber(panNumber);
            request.setPassportNumber(passportNumber);
            request.setRemarks(remarks);
            request.setAgreeToTerms(agreeToTerms);
            request.setDocuments(documents);
            request.setDocumentTypes(documentTypes);

            // Submit the application
            Applicant result = loanApplicationService.submitCompleteLoanApplication(request, applicantUsername);
            
            return ResponseEntity.ok(new MessageResponse("Loan application submitted successfully! Application ID: " + result.getApplicantId()));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error uploading documents: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Error submitting loan application: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<List<Applicant>> getAllLoanApplications() {
        try {
            List<Applicant> applications = loanApplicationService.getAllLoanApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasRole('APPLICANT') or hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<?> getLoanApplicationByUsername(@PathVariable String username) {
        try {
            Applicant application = loanApplicationService.getLoanApplicationByUsername(username);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error retrieving loan application"));
        }
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<List<Applicant>> getLoanApplicationsByStatus(@PathVariable String status) {
        try {
            List<Applicant> applications = loanApplicationService.getLoanApplicationsByStatus(status);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
