package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CrossVerificationFraudDetectionEngine {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantFinancialsRepository financialsRepository;
    private final ApplicantPropertyDetailsRepository propertyDetailsRepository;
    private final AadhaarDetailsRepository aadhaarDetailsRepository;
    private final PanDetailsRepository panDetailsRepository;
    private final PassportDetailsRepository passportDetailsRepository;
    private final OtherDocumentRepository otherDocumentRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final ApplicantCreditHistoryRepository creditHistoryRepository;
    private final LoanCollateralRepository collateralRepository;
    
    public CrossVerificationFraudDetectionEngine(
            ApplicantRepository applicantRepository,
            ApplicantBasicDetailsRepository basicDetailsRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantFinancialsRepository financialsRepository,
            ApplicantPropertyDetailsRepository propertyDetailsRepository,
            AadhaarDetailsRepository aadhaarDetailsRepository,
            PanDetailsRepository panDetailsRepository,
            PassportDetailsRepository passportDetailsRepository,
            OtherDocumentRepository otherDocumentRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            ApplicantCreditHistoryRepository creditHistoryRepository,
            LoanCollateralRepository collateralRepository) {
        this.applicantRepository = applicantRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.employmentRepository = employmentRepository;
        this.financialsRepository = financialsRepository;
        this.propertyDetailsRepository = propertyDetailsRepository;
        this.aadhaarDetailsRepository = aadhaarDetailsRepository;
        this.panDetailsRepository = panDetailsRepository;
        this.passportDetailsRepository = passportDetailsRepository;
        this.otherDocumentRepository = otherDocumentRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.collateralRepository = collateralRepository;
    }
    
    /**
     * Run all cross-verification fraud detection rules
     */
    public FraudDetectionResult detectCrossVerificationFraud(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        FraudDetectionResult result = new FraudDetectionResult();
        result.setApplicantId(applicantId);
        result.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        
        // Get all related data
        ApplicantBasicDetails basicDetails = basicDetailsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantEmployment employment = employmentRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantFinancials financials = financialsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantPropertyDetails property = propertyDetailsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantCreditHistory creditHistory = creditHistoryRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        
        List<AadhaarDetails> aadhaarList = aadhaarDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        List<PanDetails> panList = panDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        List<PassportDetails> passportList = passportDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        List<OtherDocument> documents = otherDocumentRepository
                .findByApplicant_ApplicantId(applicantId);
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        
        ApplicantLoanDetails currentLoan = loans.isEmpty() ? null : loans.get(loans.size() - 1);
        List<LoanCollateral> collaterals = currentLoan != null ? 
                collateralRepository.findByLoan_LoanId(currentLoan.getLoanId()) : new ArrayList<>();
        
        // Run all cross-verification rules
        crossVerifyIdentity(applicant, basicDetails, aadhaarList, panList, passportList, financials, result);
        crossVerifyAddress(applicant, aadhaarList, documents, result);
        crossVerifyPANAadhaar(basicDetails, aadhaarList, panList, documents, result);
        crossVerifyIncome(employment, financials, documents, result);
        crossVerifyEmployment(employment, documents, financials, result);
        crossVerifyBanking(applicant, financials, basicDetails, result);
        crossVerifyLoanLiabilities(creditHistory, financials, documents, result);
        crossVerifyProperty(applicant, property, documents, collaterals, result);
        crossVerifyGoldLoan(applicant, currentLoan, collaterals, result);
        crossVerifyBehavioral(applicant, aadhaarList, employment, result);
        
        // Calculate final risk level
        result.calculateRiskLevel();
        
        return result;
    }
    
    /**
     * 1. Identity Cross-Verification
     * Checks: Name, DOB, Father's Name, Gender across all documents
     */
    private void crossVerifyIdentity(Applicant applicant, ApplicantBasicDetails basicDetails,
                                     List<AadhaarDetails> aadhaarList, List<PanDetails> panList,
                                     List<PassportDetails> passportList, ApplicantFinancials financials,
                                     FraudDetectionResult result) {
        
        String applicantName = (applicant.getFirstName() + " " + 
                (applicant.getLastName() != null ? applicant.getLastName() : "")).trim();
        LocalDate applicantDOB = applicant.getDob();
        String applicantGender = applicant.getGender();
        
        List<String> nameSources = new ArrayList<>();
        List<String> dobSources = new ArrayList<>();
        List<String> genderSources = new ArrayList<>();
        List<String> fatherNameSources = new ArrayList<>();
        
        // Collect from all sources
        nameSources.add("Form: " + applicantName);
        if (applicantDOB != null) dobSources.add("Form: " + applicantDOB.toString());
        if (applicantGender != null) genderSources.add("Form: " + applicantGender);
        
        // Aadhaar
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getName() != null) nameSources.add("Aadhaar: " + aadhaar.getName());
            if (aadhaar.getDob() != null) dobSources.add("Aadhaar: " + aadhaar.getDob().toString());
            if (aadhaar.getGender() != null) genderSources.add("Aadhaar: " + aadhaar.getGender());
        }
        
        // PAN
        for (PanDetails pan : panList) {
            if (pan.getName() != null) nameSources.add("PAN: " + pan.getName());
            if (pan.getDob() != null) dobSources.add("PAN: " + pan.getDob().toString());
            if (pan.getFatherName() != null) fatherNameSources.add("PAN: " + pan.getFatherName());
        }
        
        // Passport
        for (PassportDetails passport : passportList) {
            if (passport.getName() != null) nameSources.add("Passport: " + passport.getName());
            if (passport.getDob() != null) dobSources.add("Passport: " + passport.getDob().toString());
        }
        
        // Bank Account Holder Name (from financials or documents)
        List<OtherDocument> bankStatements = otherDocumentRepository
                .findByApplicant_ApplicantId(applicant.getApplicantId()).stream()
                .filter(doc -> "bank_statement".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument bankDoc : bankStatements) {
            if (bankDoc.getOcrText() != null && bankDoc.getOcrText().toLowerCase().contains("account holder")) {
                // Extract name from bank statement OCR
                String extractedName = extractNameFromBankStatement(bankDoc.getOcrText());
                if (extractedName != null) {
                    nameSources.add("Bank: " + extractedName);
                }
            }
        }
        
        // Check for inconsistencies
        if (nameSources.size() >= 3 && !allNamesMatch(nameSources)) {
            FraudRule rule = new FraudRule(
                "NAME_CROSS_VERIFICATION_FAILED",
                "Name mismatch across multiple sources: " + String.join(", ", nameSources),
                50,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Name inconsistency detected across Form, Aadhaar, PAN, Bank - Possible identity theft"
            );
            result.addTriggeredRule(rule);
        }
        
        if (dobSources.size() >= 3 && !allDOBsMatch(dobSources)) {
            FraudRule rule = new FraudRule(
                "DOB_CROSS_VERIFICATION_FAILED",
                "DOB mismatch across multiple sources: " + String.join(", ", dobSources),
                45,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Date of Birth inconsistency across Form, Aadhaar, PAN, Passport"
            );
            result.addTriggeredRule(rule);
        }
        
        if (genderSources.size() >= 2 && !allGendersMatch(genderSources)) {
            FraudRule rule = new FraudRule(
                "GENDER_CROSS_VERIFICATION_FAILED",
                "Gender mismatch: " + String.join(", ", genderSources),
                30,
                "MEDIUM",
                "CROSS_VERIFICATION",
                true,
                "Gender inconsistency between Form and Aadhaar"
            );
            result.addTriggeredRule(rule);
        }
        
        // Father's name cross-verification
        if (fatherNameSources.size() >= 2 && !allFatherNamesMatch(fatherNameSources)) {
            FraudRule rule = new FraudRule(
                "FATHER_NAME_MISMATCH",
                "Father's name mismatch across documents: " + String.join(", ", fatherNameSources),
                35,
                "MEDIUM",
                "CROSS_VERIFICATION",
                true,
                "Father's name inconsistency in PAN and other documents"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * 2. Address Cross-Verification
     */
    private void crossVerifyAddress(Applicant applicant, List<AadhaarDetails> aadhaarList,
                                    List<OtherDocument> documents, FraudDetectionResult result) {
        
        List<String> addressSources = new ArrayList<>();
        List<String> citySources = new ArrayList<>();
        List<String> stateSources = new ArrayList<>();
        
        // Form address
        if (applicant.getAddress() != null) addressSources.add("Form: " + applicant.getAddress());
        if (applicant.getCity() != null) citySources.add("Form: " + applicant.getCity());
        if (applicant.getState() != null) stateSources.add("Form: " + applicant.getState());
        
        // Aadhaar address
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getAddress() != null) {
                addressSources.add("Aadhaar: " + aadhaar.getAddress());
                // Extract city/state from Aadhaar address if possible
            }
        }
        
        // Utility bill address
        List<OtherDocument> utilityBills = documents.stream()
                .filter(doc -> doc.getDocType() != null && 
                        (doc.getDocType().toLowerCase().contains("utility") ||
                         doc.getDocType().toLowerCase().contains("electricity") ||
                         doc.getDocType().toLowerCase().contains("water")))
                .toList();
        
        for (OtherDocument bill : utilityBills) {
            if (bill.getOcrText() != null && bill.getOcrText().toLowerCase().contains("address")) {
                String extractedAddress = extractAddressFromDocument(bill.getOcrText());
                if (extractedAddress != null) {
                    addressSources.add("Utility Bill: " + extractedAddress);
                }
            }
        }
        
        // Bank statement address
        List<OtherDocument> bankStatements = documents.stream()
                .filter(doc -> "bank_statement".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument bank : bankStatements) {
            if (bank.getOcrText() != null && bank.getOcrText().toLowerCase().contains("address")) {
                String extractedAddress = extractAddressFromDocument(bank.getOcrText());
                if (extractedAddress != null) {
                    addressSources.add("Bank: " + extractedAddress);
                }
            }
        }
        
        // Check address consistency
        if (addressSources.size() >= 3 && !allAddressesMatch(addressSources)) {
            FraudRule rule = new FraudRule(
                "ADDRESS_CROSS_VERIFICATION_FAILED",
                "Address mismatch across multiple sources",
                40,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Address inconsistency: " + String.join(" | ", addressSources)
            );
            result.addTriggeredRule(rule);
        }
        
        // Check city consistency
        if (citySources.size() >= 2 && !allCitiesMatch(citySources)) {
            FraudRule rule = new FraudRule(
                "CITY_MISMATCH",
                "City mismatch: " + String.join(", ", citySources),
                25,
                "MEDIUM",
                "CROSS_VERIFICATION",
                true,
                "Declared city doesn't match Aadhaar city"
            );
            result.addTriggeredRule(rule);
        }
        
        // Check state consistency
        if (stateSources.size() >= 2 && !allStatesMatch(stateSources)) {
            FraudRule rule = new FraudRule(
                "STATE_MISMATCH",
                "State mismatch: " + String.join(", ", stateSources),
                25,
                "MEDIUM",
                "CROSS_VERIFICATION",
                true,
                "Declared state doesn't match Aadhaar state"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * 3. PAN/Aadhaar Cross-Verification
     */
    private void crossVerifyPANAadhaar(ApplicantBasicDetails basicDetails,
                                       List<AadhaarDetails> aadhaarList,
                                       List<PanDetails> panList,
                                       List<OtherDocument> documents,
                                       FraudDetectionResult result) {
        
        if (basicDetails == null) return;
        
        // PAN cross-verification
        String formPAN = basicDetails.getPanNumber();
        List<String> panSources = new ArrayList<>();
        if (formPAN != null) panSources.add("Form: " + formPAN);
        
        for (PanDetails pan : panList) {
            if (pan.getPanNumber() != null) panSources.add("PAN Doc: " + pan.getPanNumber());
        }
        
        // Check ITR for PAN
        for (OtherDocument doc : documents) {
            if ("itr".equalsIgnoreCase(doc.getDocType()) && doc.getOcrText() != null) {
                String extractedPAN = extractPANFromOCR(doc.getOcrText());
                if (extractedPAN != null) {
                    panSources.add("ITR: " + extractedPAN);
                }
            }
        }
        
        if (panSources.size() >= 2 && !allPANsMatch(panSources)) {
            FraudRule rule = new FraudRule(
                "PAN_CROSS_VERIFICATION_FAILED",
                "PAN number mismatch: " + String.join(", ", panSources),
                50,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "PAN in form doesn't match PAN in documents (ITR/PAN Card)"
            );
            result.addTriggeredRule(rule);
        }
        
        // Aadhaar cross-verification
        String formAadhaar = basicDetails.getAadhaarNumber();
        List<String> aadhaarSources = new ArrayList<>();
        if (formAadhaar != null) aadhaarSources.add("Form: " + maskAadhaar(formAadhaar));
        
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getAadhaarNumber() != null) {
                aadhaarSources.add("Aadhaar Doc: " + maskAadhaar(aadhaar.getAadhaarNumber()));
            }
        }
        
        // Check utility bills for Aadhaar
        for (OtherDocument doc : documents) {
            if (doc.getDocType() != null && doc.getDocType().toLowerCase().contains("utility") &&
                doc.getOcrText() != null) {
                String extractedAadhaar = extractAadhaarFromOCR(doc.getOcrText());
                if (extractedAadhaar != null) {
                    aadhaarSources.add("Utility Bill: " + maskAadhaar(extractedAadhaar));
                }
            }
        }
        
        if (aadhaarSources.size() >= 2 && !allAadhaarsMatch(aadhaarSources)) {
            FraudRule rule = new FraudRule(
                "AADHAAR_CROSS_VERIFICATION_FAILED",
                "Aadhaar number mismatch: " + String.join(", ", aadhaarSources),
                50,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Aadhaar in form doesn't match Aadhaar in documents"
            );
            result.addTriggeredRule(rule);
        }
        
        // PAN-Aadhaar linking check (simulated)
        if (formPAN != null && formAadhaar != null) {
            // In real implementation, call PAN-Aadhaar linking API
            // For now, check if both exist in documents
            boolean panDocExists = !panList.isEmpty();
            boolean aadhaarDocExists = !aadhaarList.isEmpty();
            
            if (!panDocExists || !aadhaarDocExists) {
                FraudRule rule = new FraudRule(
                    "PAN_AADHAAR_LINKING_UNVERIFIED",
                    "PAN-Aadhaar linking cannot be verified (missing documents)",
                    30,
                    "MEDIUM",
                    "CROSS_VERIFICATION",
                    true,
                    "Both PAN and Aadhaar documents required for linking verification"
                );
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * 4. Income Cross-Verification
     */
    private void crossVerifyIncome(ApplicantEmployment employment, ApplicantFinancials financials,
                                   List<OtherDocument> documents, FraudDetectionResult result) {
        
        if (employment == null || employment.getMonthlyIncome() == null) return;
        
        BigDecimal declaredIncome = employment.getMonthlyIncome();
        List<String> incomeSources = new ArrayList<>();
        incomeSources.add("Form: ₹" + declaredIncome);
        
        // Extract from payslip
        List<OtherDocument> payslips = documents.stream()
                .filter(doc -> "payslip".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument payslip : payslips) {
            if (payslip.getOcrText() != null) {
                BigDecimal payslipIncome = extractIncomeFromPayslip(payslip.getOcrText());
                if (payslipIncome != null) {
                    incomeSources.add("Payslip: ₹" + payslipIncome);
                }
            }
        }
        
        // Bank statement credits
        if (financials != null && financials.getTotalCreditLastMonth() != null) {
            incomeSources.add("Bank Credit: ₹" + financials.getTotalCreditLastMonth());
        }
        
        // ITR income
        List<OtherDocument> itrDocs = documents.stream()
                .filter(doc -> "itr".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument itr : itrDocs) {
            if (itr.getOcrText() != null) {
                BigDecimal itrIncome = extractIncomeFromITR(itr.getOcrText());
                if (itrIncome != null) {
                    BigDecimal monthlyITR = itrIncome.divide(new BigDecimal("12"), 2, 
                            java.math.RoundingMode.HALF_UP);
                    incomeSources.add("ITR (monthly): ₹" + monthlyITR);
                }
            }
        }
        
        // Form 16
        List<OtherDocument> form16Docs = documents.stream()
                .filter(doc -> doc.getDocType() != null && 
                        doc.getDocType().toLowerCase().contains("form16"))
                .toList();
        
        for (OtherDocument form16 : form16Docs) {
            if (form16.getOcrText() != null) {
                BigDecimal form16Income = extractIncomeFromForm16(form16.getOcrText());
                if (form16Income != null) {
                    BigDecimal monthlyForm16 = form16Income.divide(new BigDecimal("12"), 2,
                            java.math.RoundingMode.HALF_UP);
                    incomeSources.add("Form 16 (monthly): ₹" + monthlyForm16);
                }
            }
        }
        
        // Check income consistency (allow 30% variance)
        if (incomeSources.size() >= 3 && !allIncomesMatch(incomeSources, declaredIncome)) {
            FraudRule rule = new FraudRule(
                "INCOME_CROSS_VERIFICATION_FAILED",
                "Income mismatch across multiple sources",
                55,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Income inconsistency: " + String.join(" | ", incomeSources) + 
                " - Possible salary slip forgery or income inflation"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * 5. Employment Cross-Verification
     */
    private void crossVerifyEmployment(ApplicantEmployment employment, 
                                       List<OtherDocument> documents,
                                       ApplicantFinancials financials,
                                       FraudDetectionResult result) {
        
        if (employment == null || employment.getEmployerName() == null) return;
        
        String formEmployer = employment.getEmployerName().toLowerCase().trim();
        List<String> employerSources = new ArrayList<>();
        employerSources.add("Form: " + employment.getEmployerName());
        
        // Extract from payslip
        List<OtherDocument> payslips = documents.stream()
                .filter(doc -> "payslip".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument payslip : payslips) {
            if (payslip.getOcrText() != null) {
                String payslipEmployer = extractEmployerFromPayslip(payslip.getOcrText());
                if (payslipEmployer != null) {
                    employerSources.add("Payslip: " + payslipEmployer);
                }
            }
        }
        
        // Extract from bank statement transaction narratives
        List<OtherDocument> bankStatements = documents.stream()
                .filter(doc -> "bank_statement".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument bank : bankStatements) {
            if (bank.getOcrText() != null) {
                String bankEmployer = extractEmployerFromBankStatement(bank.getOcrText());
                if (bankEmployer != null) {
                    employerSources.add("Bank: " + bankEmployer);
                }
            }
        }
        
        // Check employer consistency
        if (employerSources.size() >= 2 && !allEmployersMatch(employerSources)) {
            FraudRule rule = new FraudRule(
                "EMPLOYER_CROSS_VERIFICATION_FAILED",
                "Employer name mismatch across sources",
                45,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Employer inconsistency: " + String.join(" | ", employerSources) +
                " - Possible fake employer or forged documents"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * 6. Banking Cross-Verification
     */
    private void crossVerifyBanking(Applicant applicant, ApplicantFinancials financials,
                                    ApplicantBasicDetails basicDetails, FraudDetectionResult result) {
        
        if (financials == null) return;
        
        // Bank account holder name should match applicant name
        String applicantName = (applicant.getFirstName() + " " + 
                (applicant.getLastName() != null ? applicant.getLastName() : "")).trim();
        
        // IFSC code validation (basic format check)
        if (financials.getIfscCode() != null) {
            String ifsc = financials.getIfscCode();
            Pattern ifscPattern = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
            
            if (!ifscPattern.matcher(ifsc).matches()) {
                FraudRule rule = new FraudRule(
                    "INVALID_IFSC_CODE",
                    "IFSC code " + ifsc + " is invalid",
                    30,
                    "MEDIUM",
                    "CROSS_VERIFICATION",
                    true,
                    "IFSC code format validation failed"
                );
                result.addTriggeredRule(rule);
            }
        }
        
        // Check if bank name matches IFSC
        if (financials.getBankName() != null && financials.getIfscCode() != null) {
            String bankName = financials.getBankName().toLowerCase();
            String ifscPrefix = financials.getIfscCode().substring(0, 4);
            
            // Common IFSC prefixes
            boolean ifscBankMatch = 
                (bankName.contains("hdfc") && ifscPrefix.equals("HDFC")) ||
                (bankName.contains("icici") && ifscPrefix.equals("ICIC")) ||
                (bankName.contains("sbi") && ifscPrefix.equals("SBIN")) ||
                (bankName.contains("axis") && ifscPrefix.equals("UTIB")) ||
                (bankName.contains("kotak") && ifscPrefix.equals("KKBK")) ||
                (bankName.contains("pnb") && ifscPrefix.equals("PUNB"));
            
            if (!ifscBankMatch && !bankName.contains("bank")) {
                FraudRule rule = new FraudRule(
                    "BANK_IFSC_MISMATCH",
                    "Bank name '" + financials.getBankName() + "' doesn't match IFSC prefix '" + ifscPrefix + "'",
                    25,
                    "MEDIUM",
                    "CROSS_VERIFICATION",
                    true,
                    "Bank name and IFSC code inconsistency"
                );
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * 7. Loan & Liability Cross-Verification
     */
    private void crossVerifyLoanLiabilities(ApplicantCreditHistory creditHistory,
                                           ApplicantFinancials financials,
                                           List<OtherDocument> documents,
                                           FraudDetectionResult result) {
        
        if (creditHistory == null) return;
        
        Integer declaredActiveLoans = creditHistory.getTotalActiveLoans();
        BigDecimal declaredEMI = creditHistory.getTotalMonthlyEmi();
        
        // Check bank statement for EMI transactions
        List<OtherDocument> bankStatements = documents.stream()
                .filter(doc -> "bank_statement".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument bank : bankStatements) {
            if (bank.getOcrText() != null) {
                String ocrText = bank.getOcrText().toLowerCase();
                
                // Count EMI-related transactions
                int emiCount = countEMITransactions(ocrText);
                
                // Check for "no loans" declaration but EMI visible
                if (declaredActiveLoans != null && declaredActiveLoans == 0 && emiCount > 0) {
                    FraudRule rule = new FraudRule(
                        "HIDDEN_LOANS_DETECTED",
                        "Declared 'no active loans' but " + emiCount + " EMI transactions found in bank statement",
                        50,
                        "HIGH",
                        "CROSS_VERIFICATION",
                        true,
                        "Applicant hiding existing loan obligations - Deliberate misrepresentation"
                    );
                    result.addTriggeredRule(rule);
                }
                
                // Check if declared loans don't match EMI count
                if (declaredActiveLoans != null && emiCount > 0 && 
                    Math.abs(declaredActiveLoans - emiCount) >= 2) {
                    FraudRule rule = new FraudRule(
                        "LOAN_DECLARATION_MISMATCH",
                        "Declared " + declaredActiveLoans + " loans but " + emiCount + 
                        " EMI transactions in bank statement",
                        40,
                        "HIGH",
                        "CROSS_VERIFICATION",
                        true,
                        "Mismatch between declared loans and actual EMI payments"
                    );
                    result.addTriggeredRule(rule);
                }
                
                // Check credit card transactions
                int creditCardCount = countCreditCardTransactions(ocrText);
                Integer declaredCards = creditHistory.getCreditCardCount();
                
                if (declaredCards != null && declaredCards == 0 && creditCardCount > 0) {
                    FraudRule rule = new FraudRule(
                        "HIDDEN_CREDIT_CARDS",
                        "Declared 'no credit cards' but credit card transactions found",
                        35,
                        "MEDIUM",
                        "CROSS_VERIFICATION",
                        true,
                        "Credit card usage detected despite declaring no cards"
                    );
                    result.addTriggeredRule(rule);
                }
            }
        }
    }
    
    /**
     * 8. Property Cross-Verification (for Home Loans)
     */
    private void crossVerifyProperty(Applicant applicant, ApplicantPropertyDetails property,
                                    List<OtherDocument> documents, List<LoanCollateral> collaterals,
                                    FraudDetectionResult result) {
        
        if (property == null) return;
        
        // Check property ownership documents
        List<OtherDocument> propertyDocs = documents.stream()
                .filter(doc -> doc.getDocType() != null && 
                        (doc.getDocType().toLowerCase().contains("property") ||
                         doc.getDocType().toLowerCase().contains("sale_deed") ||
                         doc.getDocType().toLowerCase().contains("ownership")))
                .toList();
        
        for (OtherDocument propDoc : propertyDocs) {
            if (propDoc.getOcrText() != null) {
                String ownerName = extractOwnerNameFromPropertyDoc(propDoc.getOcrText());
                String applicantName = (applicant.getFirstName() + " " + 
                        (applicant.getLastName() != null ? applicant.getLastName() : "")).trim();
                
                if (ownerName != null && !namesMatch(applicantName, ownerName)) {
                    FraudRule rule = new FraudRule(
                        "PROPERTY_OWNER_NAME_MISMATCH",
                        "Property owner name '" + ownerName + "' doesn't match applicant '" + applicantName + "'",
                        45,
                        "HIGH",
                        "CROSS_VERIFICATION",
                        true,
                        "Property ownership document shows different owner - Possible fraud"
                    );
                    result.addTriggeredRule(rule);
                }
            }
        }
        
        // Check property value consistency
        if (property.getPropertyValue() != null && !collaterals.isEmpty()) {
            for (LoanCollateral collateral : collaterals) {
                if ("property".equalsIgnoreCase(collateral.getCollateralType()) &&
                    collateral.getEstimatedValue() != null) {
                    
                    BigDecimal declaredValue = property.getPropertyValue();
                    BigDecimal valuationValue = collateral.getEstimatedValue();
                    
                    // Allow 20% variance
                    BigDecimal variance = declaredValue.subtract(valuationValue).abs();
                    BigDecimal variancePercent = variance.divide(declaredValue, 2, 
                            java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                    
                    if (variancePercent.compareTo(new BigDecimal("20")) > 0) {
                        FraudRule rule = new FraudRule(
                            "PROPERTY_VALUE_MISMATCH",
                            "Declared property value ₹" + declaredValue + 
                            " differs from valuation ₹" + valuationValue + " by " + variancePercent + "%",
                            35,
                            "MEDIUM",
                            "CROSS_VERIFICATION",
                            true,
                            "Property value inflation detected"
                        );
                        result.addTriggeredRule(rule);
                    }
                }
            }
        }
    }
    
    /**
     * 9. Gold Loan Cross-Verification
     */
    private void crossVerifyGoldLoan(Applicant applicant, ApplicantLoanDetails loan,
                                    List<LoanCollateral> collaterals, FraudDetectionResult result) {
        
        if (loan == null || !"gold".equalsIgnoreCase(loan.getLoanType())) return;
        
        // Check gold collateral
        List<LoanCollateral> goldCollaterals = collaterals.stream()
                .filter(c -> "gold".equalsIgnoreCase(c.getCollateralType()))
                .toList();
        
        if (goldCollaterals.isEmpty()) {
            FraudRule rule = new FraudRule(
                "GOLD_LOAN_NO_COLLATERAL",
                "Gold loan application without gold collateral details",
                40,
                "HIGH",
                "CROSS_VERIFICATION",
                true,
                "Gold loan requires gold collateral documentation"
            );
            result.addTriggeredRule(rule);
            return;
        }
        
        for (LoanCollateral gold : goldCollaterals) {
            // Check if valuation report exists
            if (gold.getValuationReportUrl() == null || gold.getValuationReportUrl().isEmpty()) {
                FraudRule rule = new FraudRule(
                    "GOLD_NO_VALUATION_REPORT",
                    "Gold collateral without valuation report",
                    35,
                    "MEDIUM",
                    "CROSS_VERIFICATION",
                    true,
                    "Gold valuation report is mandatory for gold loans"
                );
                result.addTriggeredRule(rule);
            }
            
            // Check for duplicate gold valuation reports across applicants
            if (gold.getValuationReportUrl() != null) {
                List<LoanCollateral> duplicateGold = collateralRepository.findAll().stream()
                        .filter(c -> c.getValuationReportUrl() != null &&
                                    c.getValuationReportUrl().equals(gold.getValuationReportUrl()) &&
                                    !c.getId().equals(gold.getId()))
                        .toList();
                
                if (!duplicateGold.isEmpty()) {
                    FraudRule rule = new FraudRule(
                        "DUPLICATE_GOLD_VALUATION",
                        "Same gold valuation report used in " + (duplicateGold.size() + 1) + " applications",
                        50,
                        "HIGH",
                        "CROSS_VERIFICATION",
                        true,
                        "Gold valuation slip reused across multiple applications - Fraud ring detected"
                    );
                    result.addTriggeredRule(rule);
                }
            }
        }
    }
    
    /**
     * 10. Behavioral Cross-Verification
     */
    private void crossVerifyBehavioral(Applicant applicant, List<AadhaarDetails> aadhaarList,
                                      ApplicantEmployment employment, FraudDetectionResult result) {
        
        // Phone number cross-verification
        String formPhone = applicant.getPhone();
        List<String> phoneSources = new ArrayList<>();
        if (formPhone != null) phoneSources.add("Form: " + formPhone);
        
        // Aadhaar-linked phone (if available in QR data)
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getQrCodeData() != null && aadhaar.getQrCodeData().contains("mobile")) {
                String aadhaarPhone = extractPhoneFromQR(aadhaar.getQrCodeData());
                if (aadhaarPhone != null) {
                    phoneSources.add("Aadhaar: " + aadhaarPhone);
                }
            }
        }
        
        if (phoneSources.size() >= 2 && !allPhonesMatch(phoneSources)) {
            FraudRule rule = new FraudRule(
                "PHONE_CROSS_VERIFICATION_FAILED",
                "Phone number mismatch: " + String.join(", ", phoneSources),
                30,
                "MEDIUM",
                "CROSS_VERIFICATION",
                true,
                "Phone number in form doesn't match Aadhaar-linked phone"
            );
            result.addTriggeredRule(rule);
        }
        
        // Email cross-verification
        String formEmail = applicant.getEmail();
        String employerEmail = null;
        
        if (employment != null && employment.getEmployerName() != null) {
            // Extract email from employer name if present
            Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
            java.util.regex.Matcher matcher = emailPattern.matcher(employment.getEmployerName());
            if (matcher.find()) {
                employerEmail = matcher.group(1);
            }
        }
        
        if (formEmail != null && employerEmail != null && 
            !formEmail.equalsIgnoreCase(employerEmail)) {
            
            // Check if they're from same domain
            String formDomain = formEmail.substring(formEmail.indexOf("@") + 1);
            String empDomain = employerEmail.substring(employerEmail.indexOf("@") + 1);
            
            if (!formDomain.equalsIgnoreCase(empDomain)) {
                FraudRule rule = new FraudRule(
                    "EMAIL_DOMAIN_MISMATCH",
                    "Form email domain (" + formDomain + ") doesn't match employer domain (" + empDomain + ")",
                    25,
                    "MEDIUM",
                    "CROSS_VERIFICATION",
                    true,
                    "Email domain inconsistency between form and employer"
                );
                result.addTriggeredRule(rule);
            }
        }
        
        // Multiple applications from same device/IP (simulated - would need actual tracking)
        // This would be implemented with session tracking in real system
    }
    
    // ==================== UTILITY METHODS ====================
    
    private boolean allNamesMatch(List<String> nameSources) {
        if (nameSources.size() < 2) return true;
        
        List<String> normalizedNames = nameSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase()
                        .replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " "))
                .toList();
        
        String baseName = normalizedNames.get(0);
        for (int i = 1; i < normalizedNames.size(); i++) {
            if (!namesMatch(baseName, normalizedNames.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    private boolean namesMatch(String name1, String name2) {
        name1 = name1.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
        name2 = name2.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
        
        if (name1.equals(name2)) return true;
        if (name1.contains(name2) || name2.contains(name1)) return true;
        
        String[] words1 = name1.split("\\s+");
        String[] words2 = name2.split("\\s+");
        
        int matchCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return (matchCount * 100.0 / Math.max(words1.length, words2.length)) >= 70;
    }
    
    private boolean allDOBsMatch(List<String> dobSources) {
        if (dobSources.size() < 2) return true;
        
        List<String> dates = dobSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim())
                .toList();
        
        String baseDate = dates.get(0);
        for (int i = 1; i < dates.size(); i++) {
            if (!dates.get(i).equals(baseDate)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allGendersMatch(List<String> genderSources) {
        if (genderSources.size() < 2) return true;
        
        List<String> genders = genderSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase())
                .toList();
        
        String baseGender = genders.get(0);
        for (int i = 1; i < genders.size(); i++) {
            if (!genders.get(i).equals(baseGender)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allFatherNamesMatch(List<String> fatherNameSources) {
        if (fatherNameSources.size() < 2) return true;
        
        List<String> names = fatherNameSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase())
                .toList();
        
        String baseName = names.get(0);
        for (int i = 1; i < names.size(); i++) {
            if (!namesMatch(baseName, names.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allAddressesMatch(List<String> addressSources) {
        if (addressSources.size() < 2) return true;
        
        List<String> addresses = addressSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " "))
                .toList();
        
        String baseAddress = addresses.get(0);
        for (int i = 1; i < addresses.size(); i++) {
            if (!addressesMatch(baseAddress, addresses.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    private boolean addressesMatch(String addr1, String addr2) {
        String[] words1 = addr1.split("\\s+");
        String[] words2 = addr2.split("\\s+");
        
        int matchCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return (matchCount * 100.0 / Math.max(words1.length, words2.length)) >= 60;
    }
    
    private boolean allCitiesMatch(List<String> citySources) {
        if (citySources.size() < 2) return true;
        
        List<String> cities = citySources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase())
                .toList();
        
        String baseCity = cities.get(0);
        for (int i = 1; i < cities.size(); i++) {
            if (!cities.get(i).equals(baseCity)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allStatesMatch(List<String> stateSources) {
        if (stateSources.size() < 2) return true;
        
        List<String> states = stateSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase())
                .toList();
        
        String baseState = states.get(0);
        for (int i = 1; i < states.size(); i++) {
            if (!states.get(i).equals(baseState)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allPANsMatch(List<String> panSources) {
        if (panSources.size() < 2) return true;
        
        List<String> pans = panSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toUpperCase())
                .toList();
        
        String basePAN = pans.get(0);
        for (int i = 1; i < pans.size(); i++) {
            if (!pans.get(i).equals(basePAN)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allAadhaarsMatch(List<String> aadhaarSources) {
        if (aadhaarSources.size() < 2) return true;
        
        List<String> aadhaars = aadhaarSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim()
                        .replaceAll("[^0-9]", ""))
                .toList();
        
        String baseAadhaar = aadhaars.get(0);
        for (int i = 1; i < aadhaars.size(); i++) {
            if (!aadhaars.get(i).equals(baseAadhaar)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean allIncomesMatch(List<String> incomeSources, BigDecimal declaredIncome) {
        if (incomeSources.size() < 3) return true;
        
        // Extract amounts and check variance
        for (String source : incomeSources) {
            if (!source.startsWith("Form:")) {
                String amountStr = source.substring(source.indexOf("₹") + 1).trim()
                        .replaceAll(",", "");
                try {
                    BigDecimal amount = new BigDecimal(amountStr);
                    BigDecimal variance = declaredIncome.subtract(amount).abs();
                    BigDecimal variancePercent = variance.divide(declaredIncome, 2,
                            java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                    
                    if (variancePercent.compareTo(new BigDecimal("30")) > 0) {
                        return false;
                    }
                } catch (Exception e) {
                    // Unable to parse amount
                }
            }
        }
        return true;
    }
    
    private boolean allEmployersMatch(List<String> employerSources) {
        if (employerSources.size() < 2) return true;
        
        List<String> employers = employerSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " "))
                .toList();
        
        String baseEmployer = employers.get(0);
        for (int i = 1; i < employers.size(); i++) {
            if (!employersMatch(baseEmployer, employers.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    private boolean employersMatch(String emp1, String emp2) {
        if (emp1.equals(emp2)) return true;
        if (emp1.contains(emp2) || emp2.contains(emp1)) return true;
        
        String[] words1 = emp1.split("\\s+");
        String[] words2 = emp2.split("\\s+");
        
        int matchCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 3) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return (matchCount * 100.0 / Math.max(words1.length, words2.length)) >= 60;
    }
    
    private boolean allPhonesMatch(List<String> phoneSources) {
        if (phoneSources.size() < 2) return true;
        
        List<String> phones = phoneSources.stream()
                .map(s -> s.substring(s.indexOf(":") + 1).trim().replaceAll("[^0-9]", ""))
                .toList();
        
        String basePhone = phones.get(0);
        for (int i = 1; i < phones.size(); i++) {
            if (!phones.get(i).equals(basePhone)) {
                return false;
            }
        }
        return true;
    }
    
    // ==================== EXTRACTION METHODS ====================
    
    private String extractNameFromBankStatement(String ocrText) {
        // Look for "Account Holder: Name" pattern
        Pattern pattern = Pattern.compile("account holder[:\\s]+([a-zA-Z\\s]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    private String extractAddressFromDocument(String ocrText) {
        // Simple extraction - look for "Address:" pattern
        Pattern pattern = Pattern.compile("address[:\\s]+([^\n]{20,100})", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    private String extractPANFromOCR(String ocrText) {
        Pattern pattern = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]{1}");
        java.util.regex.Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
    
    private String extractAadhaarFromOCR(String ocrText) {
        Pattern pattern = Pattern.compile("\\b[0-9]{12}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
    
    private BigDecimal extractIncomeFromPayslip(String ocrText) {
        String[] patterns = {
            "net pay[:\\s]+([0-9,]+)",
            "net salary[:\\s]+([0-9,]+)",
            "take home[:\\s]+([0-9,]+)"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(ocrText);
            if (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replaceAll(",", "");
                    return new BigDecimal(amountStr);
                } catch (Exception e) {
                    // Continue to next pattern
                }
            }
        }
        return null;
    }
    
    private BigDecimal extractIncomeFromITR(String ocrText) {
        String[] patterns = {
            "total income[:\\s]+([0-9,]+)",
            "gross income[:\\s]+([0-9,]+)",
            "taxable income[:\\s]+([0-9,]+)"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(ocrText);
            if (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replaceAll(",", "");
                    return new BigDecimal(amountStr);
                } catch (Exception e) {
                    // Continue
                }
            }
        }
        return null;
    }
    
    private BigDecimal extractIncomeFromForm16(String ocrText) {
        String[] patterns = {
            "gross salary[:\\s]+([0-9,]+)",
            "total salary[:\\s]+([0-9,]+)"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(ocrText);
            if (matcher.find()) {
                try {
                    String amountStr = matcher.group(1).replaceAll(",", "");
                    return new BigDecimal(amountStr);
                } catch (Exception e) {
                    // Continue
                }
            }
        }
        return null;
    }
    
    private String extractEmployerFromPayslip(String ocrText) {
        // Look for company name at top of payslip
        String[] lines = ocrText.split("\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            if (firstLine.length() > 5 && firstLine.length() < 100) {
                return firstLine;
            }
        }
        return null;
    }
    
    private String extractEmployerFromBankStatement(String ocrText) {
        // Look for salary credit transactions
        Pattern pattern = Pattern.compile("salary.*?from[:\\s]+([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    private String extractOwnerNameFromPropertyDoc(String ocrText) {
        Pattern pattern = Pattern.compile("owner[:\\s]+([a-zA-Z\\s]+)", Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(ocrText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    
    private String extractPhoneFromQR(String qrData) {
        Pattern pattern = Pattern.compile("\\b[0-9]{10}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(qrData);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
    
    private int countEMITransactions(String ocrText) {
        String[] emiKeywords = {"emi", "loan", "installment", "instalment", "repayment"};
        int count = 0;
        
        for (String keyword : emiKeywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(ocrText);
            while (matcher.find()) {
                count++;
            }
        }
        
        return Math.min(count, 10); // Cap at 10 to avoid over-counting
    }
    
    private int countCreditCardTransactions(String ocrText) {
        String[] ccKeywords = {"credit card", "cc payment", "card payment", "visa", "mastercard"};
        int count = 0;
        
        for (String keyword : ccKeywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(ocrText);
            while (matcher.find()) {
                count++;
            }
        }
        
        return Math.min(count / 2, 5); // Divide by 2 and cap at 5
    }
    
    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12) return aadhaar;
        return "XXXX-XXXX-" + aadhaar.substring(8);
    }
}
