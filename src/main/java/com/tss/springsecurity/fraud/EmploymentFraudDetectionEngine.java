package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmploymentFraudDetectionEngine {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final OtherDocumentRepository otherDocumentRepository;
    private final DatabaseFraudRuleEngine dbRuleEngine;
    
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
            OtherDocumentRepository otherDocumentRepository,
            DatabaseFraudRuleEngine dbRuleEngine) {
        this.applicantRepository = applicantRepository;
        this.employmentRepository = employmentRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.otherDocumentRepository = otherDocumentRepository;
        this.dbRuleEngine = dbRuleEngine;
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
        
        // Load active rules from database for EMPLOYMENT category
        Map<String, FraudRuleDefinition> rules = dbRuleEngine.getRulesAsMap("EMPLOYMENT");
        
        // Get related data
        ApplicantEmployment employment = employmentRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantBasicDetails basicDetails = basicDetailsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        List<OtherDocument> documents = otherDocumentRepository
                .findByApplicant_ApplicantId(applicantId);
        
        if (employment == null) {
            FraudRuleDefinition ruleDef = rules.get("MISSING_EMPLOYMENT_DETAILS");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Cannot verify employment without details";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
            result.calculateRiskLevel();
            return result;
        }
        
        // Run all employment fraud rules (only if enabled in database)
        checkEmployerNotInValidDB(employment, rules, result);
        checkFakeEmployerEmail(employment, rules, result);
        checkPayslipFormatting(employment, documents, rules, result);
        checkInvalidEmployerAddress(employment, rules, result);
        checkEmploymentDurationMismatch(employment, documents, rules, result);
        checkUnverifiableSelfEmployed(employment, basicDetails, documents, rules, result);
        checkGhostCompany(employment, rules, result);
        
        // Calculate final risk level
        result.calculateRiskLevel();
        
        return result;
    }
    
    /**
     * Rule 1: Employer Not in Valid Companies Database
     */
    private void checkEmployerNotInValidDB(ApplicantEmployment employment,
                                           Map<String, FraudRuleDefinition> rules,
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
                FraudRuleDefinition ruleDef = rules.get("SHELL_COMPANY_EMPLOYER");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String customDesc = "Employer '" + employment.getEmployerName() + "' shows shell company characteristics";
                    String flagDetails = "Employer not in verified companies database and shows shell company patterns";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                    result.addTriggeredRule(rule);
                }
            } else {
                FraudRuleDefinition ruleDef = rules.get("UNVERIFIED_EMPLOYER");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String customDesc = "Employer '" + employment.getEmployerName() + "' not found in verified companies database";
                    String flagDetails = "Employer legitimacy cannot be verified - requires manual verification";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
    }
    
    /**
     * Rule 2: Fake Employer Email Domain
     */
    private void checkFakeEmployerEmail(ApplicantEmployment employment,
                                        Map<String, FraudRuleDefinition> rules,
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
                FraudRuleDefinition ruleDef = rules.get("FAKE_EMPLOYER_EMAIL");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String customDesc = "Employer uses personal email domain (@" + domain + ") instead of corporate domain";
                    String flagDetails = "Legitimate companies use corporate email domains, not " + domain;
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
        
        // Check if employer name itself suggests fake email usage
        for (String invalidDomain : INVALID_EMAIL_DOMAINS) {
            if (employerName.contains(invalidDomain)) {
                FraudRuleDefinition ruleDef = rules.get("PERSONAL_EMAIL_IN_EMPLOYER");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String customDesc = "Employer name contains personal email domain: " + invalidDomain;
                    String flagDetails = "Employer contact uses personal email - likely fake employer";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                    result.addTriggeredRule(rule);
                }
                break;
            }
        }
    }
    
    /**
     * Rule 3: Payslip Formatting Mismatch
     */
    private void checkPayslipFormatting(ApplicantEmployment employment,
                                        List<OtherDocument> documents,
                                        Map<String, FraudRuleDefinition> rules,
                                        FraudDetectionResult result) {
        // Find payslip documents
        List<OtherDocument> payslips = documents.stream()
                .filter(doc -> "payslip".equalsIgnoreCase(doc.getDocType()))
                .toList();
        
        if (payslips.isEmpty()) {
            FraudRuleDefinition ruleDef = rules.get("MISSING_PAYSLIP");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Payslip required for employment verification";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
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
                    FraudRuleDefinition ruleDef = rules.get("FAKE_PAYSLIP_TEMPLATE");
                    if (ruleDef != null && ruleDef.getIsActive()) {
                        String flagDetails = "Payslip OCR detected: " + (ocrText.contains("template") ? "template" : 
                                ocrText.contains("sample") ? "sample" : "dummy") + " text";
                        FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                        result.addTriggeredRule(rule);
                    }
                }
                
                if (!hasEmployerName) {
                    FraudRuleDefinition ruleDef = rules.get("PAYSLIP_EMPLOYER_MISMATCH");
                    if (ruleDef != null && ruleDef.getIsActive()) {
                        String customDesc = "Payslip does not contain employer name: " + employment.getEmployerName();
                        String flagDetails = "Employer name missing from payslip - possible forgery";
                        FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                        result.addTriggeredRule(rule);
                    }
                }
                
                if (!hasBasicFields) {
                    FraudRuleDefinition ruleDef = rules.get("INCOMPLETE_PAYSLIP");
                    if (ruleDef != null && ruleDef.getIsActive()) {
                        String flagDetails = "Payslip format does not match standard salary slip structure";
                        FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                        result.addTriggeredRule(rule);
                    }
                }
            }
        }
    }
    
    /**
     * Rule 4: Invalid Employer Address
     */
    private void checkInvalidEmployerAddress(ApplicantEmployment employment,
                                             Map<String, FraudRuleDefinition> rules,
                                             FraudDetectionResult result) {
        if (employment.getEmployerName() == null) return;
        
        String employerName = employment.getEmployerName().toLowerCase();
        
        // Check for address-related red flags
        if (employerName.contains("residential") || employerName.contains("home address") ||
            employerName.contains("apartment") || employerName.contains("flat no")) {
            FraudRuleDefinition ruleDef = rules.get("RESIDENTIAL_EMPLOYER_ADDRESS");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Legitimate companies operate from commercial addresses";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        // Check if employer name is too generic or vague
        if (employerName.matches(".*\\b(company|firm|business|shop|store)\\b.*") && 
            employerName.length() < 20) {
            FraudRuleDefinition ruleDef = rules.get("VAGUE_EMPLOYER_NAME");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Employer name is too generic/vague: " + employment.getEmployerName();
                String flagDetails = "Generic employer names often indicate fake companies";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 5: Employment Duration Mismatch
     */
    private void checkEmploymentDurationMismatch(ApplicantEmployment employment,
                                                 List<OtherDocument> documents,
                                                 Map<String, FraudRuleDefinition> rules,
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
                        FraudRuleDefinition ruleDef = rules.get("EMPLOYMENT_DURATION_MISMATCH");
                        if (ruleDef != null && ruleDef.getIsActive()) {
                            String customDesc = "Declared employment: " + declaredYears + " years, but payslip suggests shorter duration";
                            String flagDetails = "Employment duration mismatch between application and payslip";
                            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                            result.addTriggeredRule(rule);
                        }
                    }
                }
            }
        }
        
        // Check for unrealistic employment duration
        if (declaredYears > 40) {
            FraudRuleDefinition ruleDef = rules.get("UNREALISTIC_EMPLOYMENT_DURATION");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Employment duration of " + declaredYears + " years is unrealistic";
                String flagDetails = "Employment duration exceeds reasonable working years";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        // Check if employment started in future
        if (startDate.isAfter(today)) {
            FraudRuleDefinition ruleDef = rules.get("FUTURE_EMPLOYMENT_DATE");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Employment start date is in the future: " + startDate;
                String flagDetails = "Invalid employment start date - cannot be in future";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 6: Unverifiable Self-Employed Business
     */
    private void checkUnverifiableSelfEmployed(ApplicantEmployment employment,
                                               ApplicantBasicDetails basicDetails,
                                               List<OtherDocument> documents,
                                               Map<String, FraudRuleDefinition> rules,
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
            FraudRuleDefinition ruleDef = rules.get("UNVERIFIABLE_SELF_EMPLOYED");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Self-employed applicant must provide GST registration or business license";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        if (!hasITR && employment.getMonthlyIncome() != null && 
            employment.getMonthlyIncome().doubleValue() > 50000) {
            FraudRuleDefinition ruleDef = rules.get("SELF_EMPLOYED_NO_ITR");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Self-employed applicants must file ITR for income verification";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        // Check if PAN is available (required for business)
        if (basicDetails != null && basicDetails.getPanNumber() == null) {
            FraudRuleDefinition ruleDef = rules.get("SELF_EMPLOYED_NO_PAN");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "PAN is mandatory for self-employed business owners";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 7: Ghost Company Detection
     */
    private void checkGhostCompany(ApplicantEmployment employment,
                                   Map<String, FraudRuleDefinition> rules,
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
            FraudRuleDefinition ruleDef = rules.get("GHOST_COMPANY");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Employer '" + employment.getEmployerName() + "' shows ghost company characteristics";
                String flagDetails = ghostDetails.toString() + "Company likely exists only on paper";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        } else if (ghostScore >= 15) {
            FraudRuleDefinition ruleDef = rules.get("SUSPICIOUS_EMPLOYER");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Employer '" + employment.getEmployerName() + "' shows suspicious characteristics";
                String flagDetails = ghostDetails.toString() + "Requires manual verification";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
}
