package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.CompleteLoanApplicationRequest;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import com.tss.springsecurity.service.CloudinaryService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//@Service  // Disabled until Cloudinary dependency is added
public class ComprehensiveLoanApplicationServiceImpl {

    @Autowired
    private ApplicantRepository applicantRepository;
    
    @Autowired
    private ApplicantBasicDetailsRepository basicDetailsRepository;
    
    @Autowired
    private ApplicantEmploymentRepository employmentRepository;
    
    @Autowired
    private ApplicantFinancialsRepository financialsRepository;
    
    @Autowired
    private ApplicantPropertyDetailsRepository propertyDetailsRepository;
    
    @Autowired
    private ApplicantLoanDetailsRepository loanDetailsRepository;
    
    @Autowired
    private AadhaarDetailsRepository aadhaarDetailsRepository;
    
    @Autowired
    private PanDetailsRepository panDetailsRepository;
    
    @Autowired
    private PassportDetailsRepository passportDetailsRepository;
    
    @Autowired
    private OtherDocumentRepository otherDocumentRepository;
    
    @Autowired
    private CloudinaryService cloudinaryService;

    @Transactional
    public Applicant submitCompleteLoanApplication(CompleteLoanApplicationRequest request, String applicantUsername) throws IOException {
        
        // 1. Find or create applicant
        Applicant applicant = applicantRepository.findByUsername(applicantUsername)
                .orElseThrow(() -> new RuntimeException("Applicant not found with username: " + applicantUsername));

        // Update applicant basic info if needed
        updateApplicantBasicInfo(applicant, request);
        applicant = applicantRepository.save(applicant);

        // 2. Save Basic Details
        saveBasicDetails(applicant, request);

        // 3. Save Employment Details
        saveEmploymentDetails(applicant, request);

        // 4. Save Financial Details
        saveFinancialDetails(applicant, request);

        // 5. Save Property Details (if applicable)
        if (isPropertyLoan(request.getLoanType())) {
            savePropertyDetails(applicant, request);
        }

        // 6. Save Loan Details
        saveLoanDetails(applicant, request);

        // 7. Save Identity Documents
        saveIdentityDocuments(applicant, request);

        // 8. Upload and save other documents
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            saveOtherDocuments(applicant, request);
        }

        // 9. Update applicant status
        applicant.setApprovalStatus("SUBMITTED");
        applicant.setUpdatedAt(LocalDateTime.now());
        
        return applicantRepository.save(applicant);
    }

    private void updateApplicantBasicInfo(Applicant applicant, CompleteLoanApplicationRequest request) {
        applicant.setFirstName(request.getFirstName());
        applicant.setLastName(request.getLastName());
        applicant.setEmail(request.getEmail());
        applicant.setPhone(request.getPhone());
        applicant.setAddress(request.getCurrentAddress());
        applicant.setCity(request.getCurrentCity());
        applicant.setState(request.getCurrentState());
        applicant.setCountry("India"); // Default
        applicant.setGender(request.getGender());
    }

    private void saveBasicDetails(Applicant applicant, CompleteLoanApplicationRequest request) {
        ApplicantBasicDetails basicDetails = new ApplicantBasicDetails();
        basicDetails.setApplicant(applicant);
        basicDetails.setFirstName(request.getFirstName());
        basicDetails.setMiddleName(request.getMiddleName());
        basicDetails.setLastName(request.getLastName());
        basicDetails.setDateOfBirth(request.getDateOfBirth());
        basicDetails.setGender(request.getGender());
        basicDetails.setPhone(request.getPhone());
        basicDetails.setEmail(request.getEmail());
        basicDetails.setCurrentAddress(request.getCurrentAddress());
        basicDetails.setCurrentCity(request.getCurrentCity());
        basicDetails.setCurrentState(request.getCurrentState());
        basicDetails.setCurrentPincode(request.getCurrentPincode());
        
        if (!request.getSameAsCurrent()) {
            basicDetails.setPermanentAddress(request.getPermanentAddress());
            basicDetails.setPermanentCity(request.getPermanentCity());
            basicDetails.setPermanentState(request.getPermanentState());
            basicDetails.setPermanentPincode(request.getPermanentPincode());
        } else {
            basicDetails.setPermanentAddress(request.getCurrentAddress());
            basicDetails.setPermanentCity(request.getCurrentCity());
            basicDetails.setPermanentState(request.getCurrentState());
            basicDetails.setPermanentPincode(request.getCurrentPincode());
        }
        
        basicDetailsRepository.save(basicDetails);
    }

    private void saveEmploymentDetails(Applicant applicant, CompleteLoanApplicationRequest request) {
        ApplicantEmployment employment = new ApplicantEmployment();
        employment.setApplicant(applicant);
        employment.setEmploymentType(request.getEmploymentType());
        employment.setCompanyName(request.getCompanyName());
        employment.setDesignation(request.getDesignation());
        employment.setWorkExperience(request.getWorkExperience());
        employment.setOfficeAddress(request.getOfficeAddress());
        employment.setOfficeCity(request.getOfficeCity());
        employment.setOfficeState(request.getOfficeState());
        employment.setOfficePincode(request.getOfficePincode());
        employment.setMonthlyIncome(request.getMonthlyIncome());
        
        employmentRepository.save(employment);
    }

    private void saveFinancialDetails(Applicant applicant, CompleteLoanApplicationRequest request) {
        ApplicantFinancials financials = new ApplicantFinancials();
        financials.setApplicant(applicant);
        financials.setMonthlyIncome(request.getMonthlyIncome());
        financials.setOtherIncome(request.getOtherIncome() != null ? request.getOtherIncome() : BigDecimal.ZERO);
        financials.setMonthlyExpenses(request.getMonthlyExpenses() != null ? request.getMonthlyExpenses() : BigDecimal.ZERO);
        financials.setExistingLoanEmi(request.getExistingLoanEmi() != null ? request.getExistingLoanEmi() : BigDecimal.ZERO);
        financials.setCreditCardOutstanding(request.getCreditCardOutstanding() != null ? request.getCreditCardOutstanding() : BigDecimal.ZERO);
        financials.setBankName(request.getBankName());
        financials.setAccountNumber(request.getAccountNumber());
        financials.setIfscCode(request.getIfscCode());
        financials.setAccountType(request.getAccountType());
        
        financialsRepository.save(financials);
    }

    private void savePropertyDetails(Applicant applicant, CompleteLoanApplicationRequest request) {
        ApplicantPropertyDetails propertyDetails = new ApplicantPropertyDetails();
        propertyDetails.setApplicant(applicant);
        propertyDetails.setPropertyType(request.getPropertyType());
        propertyDetails.setPropertyAddress(request.getPropertyAddress());
        propertyDetails.setPropertyCity(request.getPropertyCity());
        propertyDetails.setPropertyState(request.getPropertyState());
        propertyDetails.setPropertyPincode(request.getPropertyPincode());
        propertyDetails.setPropertyValue(request.getPropertyValue());
        propertyDetails.setConstructionStatus(request.getConstructionStatus());
        
        propertyDetailsRepository.save(propertyDetails);
    }

    private void saveLoanDetails(Applicant applicant, CompleteLoanApplicationRequest request) {
        ApplicantLoanDetails loanDetails = new ApplicantLoanDetails();
        loanDetails.setApplicant(applicant);
        loanDetails.setLoanType(request.getLoanType());
        loanDetails.setLoanAmount(request.getLoanAmount());
        loanDetails.setLoanTenure(request.getLoanTenure());
        loanDetails.setLoanPurpose(request.getLoanPurpose());
        loanDetails.setApplicationStatus("SUBMITTED");
        loanDetails.setApplicationDate(LocalDateTime.now());
        
        // Calculate EMI (simple calculation)
        BigDecimal monthlyRate = BigDecimal.valueOf(0.085).divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal emi = calculateEMI(request.getLoanAmount(), monthlyRate, request.getLoanTenure());
        loanDetails.setEmiAmount(emi);
        
        loanDetailsRepository.save(loanDetails);
    }

    private void saveIdentityDocuments(Applicant applicant, CompleteLoanApplicationRequest request) {
        // Save Aadhaar Details
        if (request.getAadhaarNumber() != null && !request.getAadhaarNumber().isEmpty()) {
            AadhaarDetails aadhaar = new AadhaarDetails();
            aadhaar.setApplicant(applicant);
            aadhaar.setAadhaarNumber(request.getAadhaarNumber());
            aadhaar.setIsVerified(false);
            aadhaar.setVerificationStatus("PENDING");
            aadhaarDetailsRepository.save(aadhaar);
        }

        // Save PAN Details
        if (request.getPanNumber() != null && !request.getPanNumber().isEmpty()) {
            PanDetails pan = new PanDetails();
            pan.setApplicant(applicant);
            pan.setPanNumber(request.getPanNumber());
            pan.setIsVerified(false);
            pan.setVerificationStatus("PENDING");
            panDetailsRepository.save(pan);
        }

        // Save Passport Details
        if (request.getPassportNumber() != null && !request.getPassportNumber().isEmpty()) {
            PassportDetails passport = new PassportDetails();
            passport.setApplicant(applicant);
            passport.setPassportNumber(request.getPassportNumber());
            passport.setIsVerified(false);
            passport.setVerificationStatus("PENDING");
            passportDetailsRepository.save(passport);
        }
    }

    private void saveOtherDocuments(Applicant applicant, CompleteLoanApplicationRequest request) throws IOException {
        List<MultipartFile> documents = request.getDocuments();
        List<String> documentTypes = request.getDocumentTypes();

        for (int i = 0; i < documents.size(); i++) {
            MultipartFile file = documents.get(i);
            String documentType = i < documentTypes.size() ? documentTypes.get(i) : "OTHER";

            if (!file.isEmpty()) {
                // Upload to Cloudinary
                String cloudinaryUrl = cloudinaryService.uploadDocument(file, documentType, applicant.getApplicantId().toString());

                // Save document info to database
                OtherDocument document = new OtherDocument();
                document.setApplicant(applicant);
                document.setDocumentType(documentType);
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentUrl(cloudinaryUrl);
                document.setFileSize(file.getSize());
                document.setMimeType(file.getContentType());
                document.setUploadedAt(LocalDateTime.now());
                document.setIsVerified(false);
                document.setVerificationStatus("PENDING");

                otherDocumentRepository.save(document);
            }
        }
    }

    private boolean isPropertyLoan(String loanType) {
        return "home_loan".equalsIgnoreCase(loanType) || 
               "home_improvement".equalsIgnoreCase(loanType) ||
               "plot_loan".equalsIgnoreCase(loanType);
    }

    private BigDecimal calculateEMI(BigDecimal principal, BigDecimal monthlyRate, Integer tenure) {
        // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenure);
        
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
    }

    public List<Applicant> getAllLoanApplications() {
        return applicantRepository.findAll();
    }

    public Applicant getLoanApplicationByUsername(String username) {
        return applicantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Loan application not found for username: " + username));
    }

    public List<Applicant> getLoanApplicationsByStatus(String status) {
        return applicantRepository.findByApprovalStatus(status);
    }
}
