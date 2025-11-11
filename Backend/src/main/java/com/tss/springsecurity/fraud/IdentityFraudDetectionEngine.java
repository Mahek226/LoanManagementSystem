package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class IdentityFraudDetectionEngine {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final AadhaarDetailsRepository aadhaarDetailsRepository;
    private final PanDetailsRepository panDetailsRepository;
    private final PassportDetailsRepository passportDetailsRepository;
    private final DatabaseFraudRuleEngine dbRuleEngine;
    
    // Regex patterns
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]{1}");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    
    public IdentityFraudDetectionEngine(
            ApplicantRepository applicantRepository,
            ApplicantBasicDetailsRepository basicDetailsRepository,
            AadhaarDetailsRepository aadhaarDetailsRepository,
            PanDetailsRepository panDetailsRepository,
            PassportDetailsRepository passportDetailsRepository,
            DatabaseFraudRuleEngine dbRuleEngine) {
        this.applicantRepository = applicantRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.aadhaarDetailsRepository = aadhaarDetailsRepository;
        this.panDetailsRepository = panDetailsRepository;
        this.passportDetailsRepository = passportDetailsRepository;
        this.dbRuleEngine = dbRuleEngine;
    }
    
    /**
     * Run all identity fraud detection rules for an applicant
     */
    public FraudDetectionResult detectIdentityFraud(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        FraudDetectionResult result = new FraudDetectionResult();
        result.setApplicantId(applicantId);
        result.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        
        // Load active rules from database for IDENTITY category
        Map<String, FraudRuleDefinition> rules = dbRuleEngine.getRulesAsMap("IDENTITY");
        
        // Get related data
        ApplicantBasicDetails basicDetails = basicDetailsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        List<AadhaarDetails> aadhaarList = aadhaarDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        List<PanDetails> panList = panDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        List<PassportDetails> passportList = passportDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        
        // Run all identity fraud rules (only if enabled in database)
        checkDuplicateAadhaar(applicant, basicDetails, rules, result);
        checkDuplicatePAN(applicant, basicDetails, rules, result);
        checkInvalidPANFormat(basicDetails, rules, result);
//        checkInvalidAadhaarNumber(basicDetails, rules, result);
        checkDOBMismatch(applicant, aadhaarList, panList, passportList, rules, result);
        checkNameMismatch(applicant, aadhaarList, panList, passportList, rules, result);
        checkGenderMismatch(applicant, aadhaarList, rules, result);
        checkExpiredPassport(passportList, rules, result);
        checkDuplicatePhoneEmail(applicant, rules, result);
        checkMinorApplicant(applicant, rules, result);
        checkSuspiciousAge(applicant, rules, result);
        checkMissingCriticalDocuments(aadhaarList, panList, rules, result);
        checkDocumentTampering(aadhaarList, panList, passportList, rules, result);
        checkAddressMismatch(applicant, aadhaarList, rules, result);
        
        // Calculate final risk level
        result.calculateRiskLevel();
        
        return result;
    }
    
    /**
     * Rule 1: Duplicate Aadhaar - Check if Aadhaar is already used by another applicant
     */
    private void checkDuplicateAadhaar(Applicant applicant, ApplicantBasicDetails basicDetails, 
                                       Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("DUPLICATE_AADHAAR");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (basicDetails == null || basicDetails.getAadhaarNumber() == null) return;
        
        List<ApplicantBasicDetails> duplicates = basicDetailsRepository.findAll().stream()
                .filter(bd -> bd.getAadhaarNumber() != null && 
                             bd.getAadhaarNumber().equals(basicDetails.getAadhaarNumber()) &&
                             !bd.getApplicant().getApplicantId().equals(applicant.getApplicantId()))
                .toList();
        
        if (!duplicates.isEmpty()) {
            String customDesc = "Aadhaar number " + maskAadhaar(basicDetails.getAadhaarNumber()) + 
                " is already used by " + duplicates.size() + " other applicant(s)";
            String flagDetails = "Duplicate Aadhaar found in " + duplicates.size() + " application(s)";
            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 2: Duplicate PAN - Check if PAN is already used by another applicant
     */
    private void checkDuplicatePAN(Applicant applicant, ApplicantBasicDetails basicDetails, 
                                   Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("DUPLICATE_PAN");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (basicDetails == null || basicDetails.getPanNumber() == null) return;
        
        List<ApplicantBasicDetails> duplicates = basicDetailsRepository.findAll().stream()
                .filter(bd -> bd.getPanNumber() != null && 
                             bd.getPanNumber().equals(basicDetails.getPanNumber()) &&
                             !bd.getApplicant().getApplicantId().equals(applicant.getApplicantId()))
                .toList();
        
        if (!duplicates.isEmpty()) {
            String customDesc = "PAN number " + basicDetails.getPanNumber() + 
                " is already used by " + duplicates.size() + " other applicant(s)";
            String flagDetails = "Duplicate PAN found in " + duplicates.size() + " application(s)";
            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 3: Invalid PAN Format
     */
    private void checkInvalidPANFormat(ApplicantBasicDetails basicDetails, 
                                       Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("INVALID_PAN_FORMAT");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (basicDetails == null || basicDetails.getPanNumber() == null) return;
        
        if (!PAN_PATTERN.matcher(basicDetails.getPanNumber()).matches()) {
            String customDesc = "PAN number " + basicDetails.getPanNumber() + " does not match valid format [A-Z]{5}[0-9]{4}[A-Z]{1}";
            String flagDetails = "Invalid PAN format detected";
            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 4: Invalid Aadhaar Number - Check format and Verhoeff algorithm
     * Fraud Points: +30 (HIGH)
     */
    private void checkInvalidAadhaarNumber(ApplicantBasicDetails basicDetails, FraudDetectionResult result) {
        if (basicDetails == null || basicDetails.getAadhaarNumber() == null) return;
        
        String aadhaar = basicDetails.getAadhaarNumber();
        
        // Check format
        if (!AADHAAR_PATTERN.matcher(aadhaar).matches()) {
            FraudRule rule = new FraudRule(
                "INVALID_AADHAAR_FORMAT",
                "Aadhaar number must be exactly 12 digits",
                30,
                "HIGH",
                "IDENTITY",
                true,
                "Invalid Aadhaar format"
            );
            result.addTriggeredRule(rule);
            return;
        }
        
        // Verify using Verhoeff algorithm
        if (!verifyAadhaarChecksum(aadhaar)) {
            FraudRule rule = new FraudRule(
                "INVALID_AADHAAR_CHECKSUM",
                "Aadhaar number " + maskAadhaar(aadhaar) + " failed Verhoeff checksum validation",
                35,
                "HIGH",
                "IDENTITY",
                true,
                "Aadhaar checksum validation failed - likely fake"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 5: DOB Mismatch across documents
     */
    private void checkDOBMismatch(Applicant applicant, List<AadhaarDetails> aadhaarList,
                                  List<PanDetails> panList, List<PassportDetails> passportList,
                                  Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("DOB_MISMATCH");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (applicant.getDob() == null) return;
        
        LocalDate applicantDOB = applicant.getDob();
        boolean mismatchFound = false;
        StringBuilder details = new StringBuilder("DOB mismatches found: ");
        
        // Check Aadhaar DOB
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getDob() != null && !aadhaar.getDob().equals(applicantDOB)) {
                mismatchFound = true;
                details.append("Aadhaar DOB (").append(aadhaar.getDob())
                       .append(") ≠ Applicant DOB (").append(applicantDOB).append("); ");
            }
        }
        
        // Check PAN DOB
        for (PanDetails pan : panList) {
            if (pan.getDob() != null && !pan.getDob().equals(applicantDOB)) {
                mismatchFound = true;
                details.append("PAN DOB (").append(pan.getDob())
                       .append(") ≠ Applicant DOB (").append(applicantDOB).append("); ");
            }
        }
        
        // Check Passport DOB
        for (PassportDetails passport : passportList) {
            if (passport.getDob() != null && !passport.getDob().equals(applicantDOB)) {
                mismatchFound = true;
                details.append("Passport DOB (").append(passport.getDob())
                       .append(") ≠ Applicant DOB (").append(applicantDOB).append("); ");
            }
        }
        
        if (mismatchFound) {
            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, details.toString());
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 6: Name Mismatch across documents
     */
    private void checkNameMismatch(Applicant applicant, List<AadhaarDetails> aadhaarList,
                                   List<PanDetails> panList, List<PassportDetails> passportList,
                                   Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("NAME_MISMATCH");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        
        String applicantName = (applicant.getFirstName() + " " + 
                               (applicant.getLastName() != null ? applicant.getLastName() : ""))
                               .trim().toLowerCase();
        
        boolean mismatchFound = false;
        StringBuilder details = new StringBuilder("Name mismatches found: ");
        
        // Check Aadhaar name
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getName() != null) {
                String aadhaarName = aadhaar.getName().trim().toLowerCase();
                if (!namesMatch(applicantName, aadhaarName)) {
                    mismatchFound = true;
                    details.append("Aadhaar (").append(aadhaar.getName())
                           .append(") ≠ Applicant (").append(applicantName).append("); ");
                }
            }
        }
        
        // Check PAN name
        for (PanDetails pan : panList) {
            if (pan.getName() != null) {
                String panName = pan.getName().trim().toLowerCase();
                if (!namesMatch(applicantName, panName)) {
                    mismatchFound = true;
                    details.append("PAN (").append(pan.getName())
                           .append(") ≠ Applicant (").append(applicantName).append("); ");
                }
            }
        }
        
        // Check Passport name
        for (PassportDetails passport : passportList) {
            if (passport.getName() != null) {
                String passportName = passport.getName().trim().toLowerCase();
                if (!namesMatch(applicantName, passportName)) {
                    mismatchFound = true;
                    details.append("Passport (").append(passport.getName())
                           .append(") ≠ Applicant (").append(applicantName).append("); ");
                }
            }
        }
        
        if (mismatchFound) {
            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, details.toString());
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 7: Gender Mismatch
     */
    private void checkGenderMismatch(Applicant applicant, List<AadhaarDetails> aadhaarList,
                                    Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("GENDER_MISMATCH");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (applicant.getGender() == null) return;
        
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getGender() != null && 
                !aadhaar.getGender().equalsIgnoreCase(applicant.getGender())) {
                String customDesc = "Gender mismatch: Applicant (" + applicant.getGender() + 
                    ") ≠ Aadhaar (" + aadhaar.getGender() + ")";
                String flagDetails = "Gender mismatch between applicant and Aadhaar";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
                break;
            }
        }
    }
    
    /**
     * Rule 8: Expired Passport
     */
    private void checkExpiredPassport(List<PassportDetails> passportList, 
                                     Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("EXPIRED_PASSPORT");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        
        LocalDate today = LocalDate.now();
        
        for (PassportDetails passport : passportList) {
            if (passport.getExpiryDate() != null && passport.getExpiryDate().isBefore(today)) {
                String customDesc = "Passport " + passport.getPassportNumber() + " expired on " + passport.getExpiryDate();
                String flagDetails = "Using expired passport as identity proof";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 9: Duplicate Phone/Email - Multiple applicants with same contact
     */
    private void checkDuplicatePhoneEmail(Applicant applicant, Map<String, FraudRuleDefinition> rules, 
                                         FraudDetectionResult result) {
        // Check duplicate phone
        if (applicant.getPhone() != null) {
            FraudRuleDefinition ruleDef = rules.get("DUPLICATE_PHONE");
            if (ruleDef != null && ruleDef.getIsActive()) {
                List<Applicant> phoneMatches = applicantRepository.findAll().stream()
                        .filter(a -> a.getPhone() != null && 
                                    a.getPhone().equals(applicant.getPhone()) &&
                                    !a.getApplicantId().equals(applicant.getApplicantId()))
                        .toList();
                
                if (!phoneMatches.isEmpty()) {
                    String customDesc = "Phone number " + applicant.getPhone() + " is used by " + 
                        phoneMatches.size() + " other applicant(s)";
                    String flagDetails = "Possible synthetic identity fraud - shared phone number";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
        
        // Check duplicate email
        if (applicant.getEmail() != null) {
            FraudRuleDefinition ruleDef = rules.get("DUPLICATE_EMAIL");
            if (ruleDef != null && ruleDef.getIsActive()) {
                List<Applicant> emailMatches = applicantRepository.findAll().stream()
                        .filter(a -> a.getEmail() != null && 
                                    a.getEmail().equalsIgnoreCase(applicant.getEmail()) &&
                                    !a.getApplicantId().equals(applicant.getApplicantId()))
                        .toList();
                
                if (!emailMatches.isEmpty()) {
                    String customDesc = "Email " + applicant.getEmail() + " is used by " + 
                        emailMatches.size() + " other applicant(s)";
                    String flagDetails = "Possible synthetic identity fraud - shared email";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
    }
    
    /**
     * Rule 10: Minor Applicant - Age below 18
     */
    private void checkMinorApplicant(Applicant applicant, Map<String, FraudRuleDefinition> rules, 
                                    FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("MINOR_APPLICANT");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (applicant.getDob() == null) return;
        
        int age = Period.between(applicant.getDob(), LocalDate.now()).getYears();
        
        if (age < 18) {
            String customDesc = "Applicant is " + age + " years old (below 18 years)";
            String flagDetails = "Minor cannot apply for loan independently";
            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 11: Suspicious Age - Too young or too old
     */
    private void checkSuspiciousAge(Applicant applicant, Map<String, FraudRuleDefinition> rules, 
                                   FraudDetectionResult result) {
        if (applicant.getDob() == null) return;
        
        int age = Period.between(applicant.getDob(), LocalDate.now()).getYears();
        
        if (age > 80) {
            FraudRuleDefinition ruleDef = rules.get("SUSPICIOUS_AGE_HIGH");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Applicant age is " + age + " years (above 80)";
                String flagDetails = "Unusually high age for loan applicant";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        } else if (age >= 18 && age < 21) {
            FraudRuleDefinition ruleDef = rules.get("SUSPICIOUS_AGE_LOW");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Applicant age is " + age + " years (18-20 range)";
                String flagDetails = "Young applicant - higher risk profile";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 12: Missing Critical Documents
     */
    private void checkMissingCriticalDocuments(List<AadhaarDetails> aadhaarList, 
                                               List<PanDetails> panList,
                                               Map<String, FraudRuleDefinition> rules,
                                               FraudDetectionResult result) {
        if (aadhaarList.isEmpty()) {
            FraudRuleDefinition ruleDef = rules.get("MISSING_AADHAAR");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Mandatory Aadhaar document missing";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        if (panList.isEmpty()) {
            FraudRuleDefinition ruleDef = rules.get("MISSING_PAN");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Mandatory PAN document missing";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 13: Document Tampering Detected
     */
    private void checkDocumentTampering(List<AadhaarDetails> aadhaarList, 
                                       List<PanDetails> panList,
                                       List<PassportDetails> passportList,
                                       Map<String, FraudRuleDefinition> rules,
                                       FraudDetectionResult result) {
        // Check Aadhaar tampering
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (Boolean.TRUE.equals(aadhaar.getIsTampered())) {
                FraudRuleDefinition ruleDef = rules.get("AADHAAR_TAMPERED");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String flagDetails = "Aadhaar document tampering detected";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
        
        // Check PAN tampering
        for (PanDetails pan : panList) {
            if (Boolean.TRUE.equals(pan.getIsTampered())) {
                FraudRuleDefinition ruleDef = rules.get("PAN_TAMPERED");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String flagDetails = "PAN document tampering detected";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
        
        // Check Passport tampering
        for (PassportDetails passport : passportList) {
            if (Boolean.TRUE.equals(passport.getIsTampered())) {
                FraudRuleDefinition ruleDef = rules.get("PASSPORT_TAMPERED");
                if (ruleDef != null && ruleDef.getIsActive()) {
                    String flagDetails = "Passport document tampering detected";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                    result.addTriggeredRule(rule);
                }
            }
        }
    }
    
    /**
     * Rule 14: Address Mismatch between Applicant and Aadhaar
     */
    private void checkAddressMismatch(Applicant applicant, List<AadhaarDetails> aadhaarList,
                                     Map<String, FraudRuleDefinition> rules, FraudDetectionResult result) {
        FraudRuleDefinition ruleDef = rules.get("ADDRESS_MISMATCH");
        if (ruleDef == null || !ruleDef.getIsActive()) return;
        if (applicant.getAddress() == null) return;
        
        String applicantAddress = applicant.getAddress().trim().toLowerCase();
        
        for (AadhaarDetails aadhaar : aadhaarList) {
            if (aadhaar.getAddress() != null) {
                String aadhaarAddress = aadhaar.getAddress().trim().toLowerCase();
                
                // Simple similarity check - can be enhanced with fuzzy matching
                if (!addressesMatch(applicantAddress, aadhaarAddress)) {
                    String flagDetails = "Applicant address does not match Aadhaar address";
                    FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                    result.addTriggeredRule(rule);
                    break;
                }
            }
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Verify Aadhaar using Verhoeff algorithm
     */
    private boolean verifyAadhaarChecksum(String aadhaar) {
        // Verhoeff algorithm implementation
        int[][] d = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
            {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
            {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
            {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
            {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
            {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
            {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
            {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
        };
        
        int[][] p = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
            {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
            {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
            {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
            {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
            {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
            {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
            {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
        };
        
        int c = 0;
        int[] myArray = aadhaar.chars().map(x -> x - '0').toArray();
        
        for (int i = myArray.length - 1; i >= 0; i--) {
            c = d[c][p[(myArray.length - i) % 8][myArray[i]]];
        }
        
        return c == 0;
    }
    
    /**
     * Check if two names match (allowing for minor variations)
     */
    private boolean namesMatch(String name1, String name2) {
        // Remove special characters and extra spaces
        name1 = name1.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
        name2 = name2.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
        
        // Exact match
        if (name1.equals(name2)) return true;
        
        // Check if one name contains the other (for middle name variations)
        if (name1.contains(name2) || name2.contains(name1)) return true;
        
        // Check word-by-word match (at least 70% words should match)
        String[] words1 = name1.split("\\s+");
        String[] words2 = name2.split("\\s+");
        
        int matchCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    matchCount++;
                    break;
                }
            }
        }
        
        int totalWords = Math.max(words1.length, words2.length);
        return (matchCount * 100.0 / totalWords) >= 70;
    }
    
    /**
     * Check if two addresses match (allowing for variations)
     */
    private boolean addressesMatch(String addr1, String addr2) {
        // Remove special characters and normalize
        addr1 = addr1.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
        addr2 = addr2.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
        
        // Exact match
        if (addr1.equals(addr2)) return true;
        
        // Check if one contains the other
        if (addr1.contains(addr2) || addr2.contains(addr1)) return true;
        
        // Check word-by-word match (at least 60% words should match)
        String[] words1 = addr1.split("\\s+");
        String[] words2 = addr2.split("\\s+");
        
        int matchCount = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) { // Ignore small words
                    matchCount++;
                    break;
                }
            }
        }
        
        int totalWords = Math.max(words1.length, words2.length);
        return (matchCount * 100.0 / totalWords) >= 60;
    }
    
    /**
     * Mask Aadhaar number for display (show only last 4 digits)
     */
    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12) return aadhaar;
        return "XXXX-XXXX-" + aadhaar.substring(8);
    }
}
