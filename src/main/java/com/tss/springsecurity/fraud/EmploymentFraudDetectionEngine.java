package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EmploymentFraudDetectionEngine {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final OtherDocumentRepository otherDocumentRepository;
    
    // Valid corporate email domains (can be expanded)
    private static final List<String> INVALID_EMAIL_DOMAINS = Arrays.asList(
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", 
        "rediffmail.com", "ymail.com", "aol.com", "mail.com",
        "protonmail.com", "zoho.com", "icloud.com"
    );
    
    // Known legitimate companies (sample - should be loaded from database)
    private static final List<String> KNOWN_COMPANIES = Arrays.asList(
        "tcs", "infosys", "wipro", "cognizant", "hcl", "tech mahindra",
        "accenture", "ibm", "microsoft", "google", "amazon", "flipkart",
        "hdfc", "icici", "sbi", "axis", "kotak", "reliance", "tata"
    );
    
    // Shell company indicators
    private static final List<String> SHELL_COMPANY_KEYWORDS = Arrays.asList(
        "consultancy", "services pvt ltd", "solutions pvt ltd", "enterprises",
        "trading", "exports", "imports", "ventures", "holdings"
    );
    
    public EmploymentFraudDetectionEngine(
            ApplicantRepository applicantRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantBasicDetailsRepository basicDetailsRepository,
            OtherDocumentRepository otherDocumentRepository) {
        this.applicantRepository = applicantRepository;
        this.employmentRepository = employmentRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.otherDocumentRepository = otherDocumentRepository;
    }
    
    /**
     * Run all employment fraud detection rules for an applicant
     */
    public FraudDetectionResult detectEmploymentFraud(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        FraudDetectionResult result = new FraudDetectionResult();
        result.setApplicantId(applicantId);
        result.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        
        // Get related data
        ApplicantEmployment employment = employmentRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantBasicDetails basicDetails = basicDetailsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        List<OtherDocument> documents = otherDocumentRepository
                .findByApplicant_ApplicantId(applicantId);
        
        if (employment == null) {
            FraudRule rule = new FraudRule(
                "MISSING_EMPLOYMENT_DETAILS",
                "Employment details not provided",
                30,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "Cannot verify employment without details"
            );
            result.addTriggeredRule(rule);
            result.calculateRiskLevel();
            return result;
        }
        
        // Run all employment fraud rules
        checkEmployerNotInValidDB(employment, result);
        checkFakeEmployerEmail(employment, result);
        checkPayslipFormatting(employment, documents, result);
        checkInvalidEmployerAddress(employment, result);
        checkEmploymentDurationMismatch(employment, documents, result);
        checkUnverifiableSelfEmployed(employment, basicDetails, documents, result);
        checkGhostCompany(employment, result);
        
        // Calculate final risk level
        result.calculateRiskLevel();
        
        return result;
    }
    
    /**
     * Rule 1: Employer Not in Valid Companies Database
     * Fraud Points: +50 (HIGH)
     */
    private void checkEmployerNotInValidDB(ApplicantEmployment employment,
                                           FraudDetectionResult result) {
        if (employment.getEmployerName() == null) return;
        
        String employerName = employment.getEmployerName().toLowerCase().trim();
        
        // Check if employer is a known legitimate company
        boolean isKnownCompany = KNOWN_COMPANIES.stream()
                .anyMatch(company -> employerName.contains(company));
        
        if (!isKnownCompany && !"self-employed".equalsIgnoreCase(employment.getEmploymentType())) {
            // Check for shell company indicators
            boolean hasShellIndicators = SHELL_COMPANY_KEYWORDS.stream()
                    .anyMatch(keyword -> employerName.contains(keyword));
            
            if (hasShellIndicators) {
                FraudRule rule = new FraudRule(
                    "SHELL_COMPANY_EMPLOYER",
                    "Employer '" + employment.getEmployerName() + "' shows shell company characteristics",
                    50,
                    "HIGH",
                    "EMPLOYMENT",
                    true,
                    "Employer not in verified companies database and shows shell company patterns"
                );
                result.addTriggeredRule(rule);
            } else {
                FraudRule rule = new FraudRule(
                    "UNVERIFIED_EMPLOYER",
                    "Employer '" + employment.getEmployerName() + "' not found in verified companies database",
                    35,
                    "MEDIUM",
                    "EMPLOYMENT",
                    true,
                    "Employer legitimacy cannot be verified - requires manual verification"
                );
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 2: Fake Employer Email Domain
     * Fraud Points: +45 (HIGH)
     */
    private void checkFakeEmployerEmail(ApplicantEmployment employment,
                                        FraudDetectionResult result) {
        if (employment.getEmployerName() == null) return;
        
        // Extract email domain from employer name or check if generic email is being used
        String employerName = employment.getEmployerName().toLowerCase();
        
        // Check if employer name contains email-like patterns
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        java.util.regex.Matcher matcher = emailPattern.matcher(employerName);
        
        if (matcher.find()) {
            String email = matcher.group(1);
            String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
            
            if (INVALID_EMAIL_DOMAINS.contains(domain)) {
                FraudRule rule = new FraudRule(
                    "FAKE_EMPLOYER_EMAIL",
                    "Employer uses personal email domain (@" + domain + ") instead of corporate domain",
                    45,
                    "HIGH",
                    "EMPLOYMENT",
                    true,
                    "Legitimate companies use corporate email domains, not " + domain
                );
                result.addTriggeredRule(rule);
            }
        }
        
        // Check if employer name itself suggests fake email usage
        for (String invalidDomain : INVALID_EMAIL_DOMAINS) {
            if (employerName.contains(invalidDomain)) {
                FraudRule rule = new FraudRule(
                    "PERSONAL_EMAIL_IN_EMPLOYER",
                    "Employer name contains personal email domain: " + invalidDomain,
                    40,
                    "HIGH",
                    "EMPLOYMENT",
                    true,
                    "Employer contact uses personal email - likely fake employer"
                );
                result.addTriggeredRule(rule);
                break;
            }
        }
    }
    
    /**
     * Rule 3: Payslip Formatting Mismatch
     * Fraud Points: +40 (HIGH)
     */
    private void checkPayslipFormatting(ApplicantEmployment employment,
                                        List<OtherDocument> documents,
                                        FraudDetectionResult result) {
        // Find payslip documents
        List<OtherDocument> payslips = documents.stream()
                .filter(doc -> "payslip".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        if (payslips.isEmpty()) {
            FraudRule rule = new FraudRule(
                "MISSING_PAYSLIP",
                "No payslip document provided for verification",
                25,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "Payslip required for employment verification"
            );
            result.addTriggeredRule(rule);
            return;
        }
        
        for (OtherDocument payslip : payslips) {
            if (payslip.getOcrText() != null) {
                String ocrText = payslip.getOcrText().toLowerCase();
                
                // Check for common payslip formatting issues
                boolean hasEmployerName = employment.getEmployerName() != null && 
                        ocrText.contains(employment.getEmployerName().toLowerCase());
                boolean hasBasicFields = ocrText.contains("basic") || ocrText.contains("gross") || 
                        ocrText.contains("deduction") || ocrText.contains("net pay");
                boolean hasSuspiciousText = ocrText.contains("template") || ocrText.contains("sample") ||
                        ocrText.contains("dummy") || ocrText.contains("example");
                
                if (hasSuspiciousText) {
                    FraudRule rule = new FraudRule(
                        "FAKE_PAYSLIP_TEMPLATE",
                        "Payslip contains template/sample text - likely forged",
                        50,
                        "HIGH",
                        "EMPLOYMENT",
                        true,
                        "Payslip OCR detected: " + (ocrText.contains("template") ? "template" : 
                                ocrText.contains("sample") ? "sample" : "dummy") + " text"
                    );
                    result.addTriggeredRule(rule);
                }
                
                if (!hasEmployerName) {
                    FraudRule rule = new FraudRule(
                        "PAYSLIP_EMPLOYER_MISMATCH",
                        "Payslip does not contain employer name: " + employment.getEmployerName(),
                        40,
                        "HIGH",
                        "EMPLOYMENT",
                        true,
                        "Employer name missing from payslip - possible forgery"
                    );
                    result.addTriggeredRule(rule);
                }
                
                if (!hasBasicFields) {
                    FraudRule rule = new FraudRule(
                        "INCOMPLETE_PAYSLIP",
                        "Payslip missing standard fields (Basic/Gross/Deductions/Net Pay)",
                        30,
                        "MEDIUM",
                        "EMPLOYMENT",
                        true,
                        "Payslip format does not match standard salary slip structure"
                    );
                    result.addTriggeredRule(rule);
                }
            }
        }
    }
    
    /**
     * Rule 4: Invalid Employer Address
     * Fraud Points: +35 (MEDIUM)
     */
    private void checkInvalidEmployerAddress(ApplicantEmployment employment,
                                             FraudDetectionResult result) {
        if (employment.getEmployerName() == null) return;
        
        String employerName = employment.getEmployerName().toLowerCase();
        
        // Check for address-related red flags
        if (employerName.contains("residential") || employerName.contains("home address") ||
            employerName.contains("apartment") || employerName.contains("flat no")) {
            FraudRule rule = new FraudRule(
                "RESIDENTIAL_EMPLOYER_ADDRESS",
                "Employer address appears to be residential, not commercial",
                35,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "Legitimate companies operate from commercial addresses"
            );
            result.addTriggeredRule(rule);
        }
        
        // Check if employer name is too generic or vague
        if (employerName.matches(".*\\b(company|firm|business|shop|store)\\b.*") && 
            employerName.length() < 20) {
            FraudRule rule = new FraudRule(
                "VAGUE_EMPLOYER_NAME",
                "Employer name is too generic/vague: " + employment.getEmployerName(),
                25,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "Generic employer names often indicate fake companies"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 5: Employment Duration Mismatch
     * Fraud Points: +45 (HIGH)
     */
    private void checkEmploymentDurationMismatch(ApplicantEmployment employment,
                                                 List<OtherDocument> documents,
                                                 FraudDetectionResult result) {
        if (employment.getStartDate() == null) return;
        
        // Calculate declared employment duration
        LocalDate startDate = employment.getStartDate();
        LocalDate today = LocalDate.now();
        int declaredYears = Period.between(startDate, today).getYears();
        int declaredMonths = Period.between(startDate, today).getMonths();
        
        // Check payslips for employment duration verification
        List<OtherDocument> payslips = documents.stream()
                .filter(doc -> "payslip".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        for (OtherDocument payslip : payslips) {
            if (payslip.getOcrText() != null) {
                String ocrText = payslip.getOcrText().toLowerCase();
                
                // Try to extract employment duration from payslip
                // Look for patterns like "employee since 2020" or "joining date: 01/01/2020"
                if (ocrText.contains("employee since") || ocrText.contains("joining date") ||
                    ocrText.contains("date of joining")) {
                    
                    // Simple check: if payslip is recent but shows very short employment
                    if (declaredYears >= 3 && ocrText.contains("month")) {
                        FraudRule rule = new FraudRule(
                            "EMPLOYMENT_DURATION_MISMATCH",
                            "Declared employment: " + declaredYears + " years, but payslip suggests shorter duration",
                            45,
                            "HIGH",
                            "EMPLOYMENT",
                            true,
                            "Employment duration mismatch between application and payslip"
                        );
                        result.addTriggeredRule(rule);
                    }
                }
            }
        }
        
        // Check for unrealistic employment duration
        if (declaredYears > 40) {
            FraudRule rule = new FraudRule(
                "UNREALISTIC_EMPLOYMENT_DURATION",
                "Employment duration of " + declaredYears + " years is unrealistic",
                30,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "Employment duration exceeds reasonable working years"
            );
            result.addTriggeredRule(rule);
        }
        
        // Check if employment started in future
        if (startDate.isAfter(today)) {
            FraudRule rule = new FraudRule(
                "FUTURE_EMPLOYMENT_DATE",
                "Employment start date is in the future: " + startDate,
                50,
                "CRITICAL",
                "EMPLOYMENT",
                true,
                "Invalid employment start date - cannot be in future"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 6: Unverifiable Self-Employed Business
     * Fraud Points: +40 (HIGH)
     */
    private void checkUnverifiableSelfEmployed(ApplicantEmployment employment,
                                               ApplicantBasicDetails basicDetails,
                                               List<OtherDocument> documents,
                                               FraudDetectionResult result) {
        if (!"self-employed".equalsIgnoreCase(employment.getEmploymentType())) return;
        
        // Check for GST registration
        boolean hasGSTDoc = documents.stream()
                .anyMatch(doc -> doc.getDocType() != null && 
                        doc.getDocType().toLowerCase().contains("gst"));
        
        // Check for business registration documents
        boolean hasBusinessDoc = documents.stream()
                .anyMatch(doc -> doc.getDocType() != null && 
                        (doc.getDocType().toLowerCase().contains("business") ||
                         doc.getDocType().toLowerCase().contains("registration") ||
                         doc.getDocType().toLowerCase().contains("license")));
        
        // Check for ITR (already checked in financial rules, but important for self-employed)
        boolean hasITR = documents.stream()
                .anyMatch(doc -> "itr".equalsIgnoreCase(doc.getDocType()));
        
        if (!hasGSTDoc && !hasBusinessDoc) {
            FraudRule rule = new FraudRule(
                "UNVERIFIABLE_SELF_EMPLOYED",
                "Self-employed business not registered (No GST/Business Registration)",
                40,
                "HIGH",
                "EMPLOYMENT",
                true,
                "Self-employed applicant must provide GST registration or business license"
            );
            result.addTriggeredRule(rule);
        }
        
        if (!hasITR && employment.getMonthlyIncome() != null && 
            employment.getMonthlyIncome().doubleValue() > 50000) {
            FraudRule rule = new FraudRule(
                "SELF_EMPLOYED_NO_ITR",
                "Self-employed with high income but no ITR filed",
                35,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "Self-employed applicants must file ITR for income verification"
            );
            result.addTriggeredRule(rule);
        }
        
        // Check if PAN is available (required for business)
        if (basicDetails != null && basicDetails.getPanNumber() == null) {
            FraudRule rule = new FraudRule(
                "SELF_EMPLOYED_NO_PAN",
                "Self-employed applicant without PAN number",
                30,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                "PAN is mandatory for self-employed business owners"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 7: Ghost Company Detection
     * Fraud Points: +50 (HIGH)
     */
    private void checkGhostCompany(ApplicantEmployment employment,
                                   FraudDetectionResult result) {
        if (employment.getEmployerName() == null) return;
        if ("self-employed".equalsIgnoreCase(employment.getEmploymentType())) return;
        
        String employerName = employment.getEmployerName().toLowerCase().trim();
        
        // Ghost company indicators
        boolean hasMultipleShellKeywords = SHELL_COMPANY_KEYWORDS.stream()
                .filter(keyword -> employerName.contains(keyword))
                .count() >= 2;
        
        // Check for suspicious patterns
        boolean hasNumbersOnly = employerName.matches(".*\\d{5,}.*"); // Contains 5+ consecutive digits
        boolean tooShort = employerName.length() < 10 && !KNOWN_COMPANIES.contains(employerName);
        boolean hasSpecialChars = employerName.matches(".*[#$%&*@!].*");
        
        int ghostScore = 0;
        StringBuilder ghostDetails = new StringBuilder("Ghost company indicators: ");
        
        if (hasMultipleShellKeywords) {
            ghostScore += 20;
            ghostDetails.append("Multiple shell company keywords; ");
        }
        
        if (hasNumbersOnly) {
            ghostScore += 15;
            ghostDetails.append("Suspicious number pattern; ");
        }
        
        if (tooShort) {
            ghostScore += 10;
            ghostDetails.append("Unusually short name; ");
        }
        
        if (hasSpecialChars) {
            ghostScore += 10;
            ghostDetails.append("Special characters in name; ");
        }
        
        // Check if employer name is too generic
        String[] genericWords = {"company", "firm", "business", "enterprise", "services", "solutions"};
        long genericWordCount = Arrays.stream(genericWords)
                .filter(employerName::contains)
                .count();
        
        if (genericWordCount >= 2) {
            ghostScore += 15;
            ghostDetails.append("Too generic name; ");
        }
        
        if (ghostScore >= 30) {
            FraudRule rule = new FraudRule(
                "GHOST_COMPANY",
                "Employer '" + employment.getEmployerName() + "' shows ghost company characteristics",
                50,
                "HIGH",
                "EMPLOYMENT",
                true,
                ghostDetails.toString() + "Company likely exists only on paper"
            );
            result.addTriggeredRule(rule);
        } else if (ghostScore >= 15) {
            FraudRule rule = new FraudRule(
                "SUSPICIOUS_EMPLOYER",
                "Employer '" + employment.getEmployerName() + "' shows suspicious characteristics",
                30,
                "MEDIUM",
                "EMPLOYMENT",
                true,
                ghostDetails.toString() + "Requires manual verification"
            );
            result.addTriggeredRule(rule);
        }
    }
}
