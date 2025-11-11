//package com.tss.springsecurity.service;
//
//import com.tss.springsecurity.entity.*;
//import com.tss.springsecurity.fraud.FraudDetectionResult;
//import com.tss.springsecurity.fraud.FraudRule;
//import com.tss.springsecurity.repository.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.Period;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//
///**
// * Database-driven fraud rule engine that fetches rules from database
// * instead of hard-coded logic
// */
//@Service
//@Slf4j
//public class DatabaseDrivenFraudRuleEngine {
//    
//    private final FraudRuleService fraudRuleService;
//    private final ApplicantRepository applicantRepository;
//    private final ApplicantBasicDetailsRepository basicDetailsRepository;
//    private final ApplicantEmploymentRepository employmentRepository;
//    private final ApplicantFinancialsRepository financialsRepository;
//    private final ApplicantCreditHistoryRepository creditHistoryRepository;
//    
//    public DatabaseDrivenFraudRuleEngine(
//            FraudRuleService fraudRuleService,
//            ApplicantRepository applicantRepository,
//            ApplicantBasicDetailsRepository basicDetailsRepository,
//            ApplicantEmploymentRepository employmentRepository,
//            ApplicantFinancialsRepository financialsRepository,
//            ApplicantCreditHistoryRepository creditHistoryRepository) {
//        this.fraudRuleService = fraudRuleService;
//        this.applicantRepository = applicantRepository;
//        this.basicDetailsRepository = basicDetailsRepository;
//        this.employmentRepository = employmentRepository;
//        this.financialsRepository = financialsRepository;
//        this.creditHistoryRepository = creditHistoryRepository;
//    }
//    
//    /**
//     * Detect identity fraud using database rules
//     */
//    public FraudDetectionResult detectIdentityFraud(Long applicantId) {
//        long startTime = System.currentTimeMillis();
//        
//        Applicant applicant = applicantRepository.findById(applicantId)
//                .orElseThrow(() -> new RuntimeException("Applicant not found"));
//        
//        FraudDetectionResult result = new FraudDetectionResult();
//        result.setApplicantId(applicantId);
//        result.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
//        
//        // Get all active identity rules from database
//        Map<String, FraudRuleDefinition> rules = fraudRuleService.getRulesMapByCategory("IDENTITY");
//        
//        // Get related data
//        ApplicantBasicDetails basicDetails = basicDetailsRepository
//                .findByApplicant_ApplicantId(applicantId).orElse(null);
//        
//        // Execute each rule
//        for (FraudRuleDefinition ruleDefinition : rules.values()) {
//            long ruleStartTime = System.currentTimeMillis();
//            
//            try {
//                FraudRule triggeredRule = executeIdentityRule(ruleDefinition, applicant, basicDetails);
//                
//                if (triggeredRule != null) {
//                    result.addTriggeredRule(triggeredRule);
//                    
//                    // Log execution
//                    int executionTime = (int) (System.currentTimeMillis() - ruleStartTime);
//                    fraudRuleService.logRuleExecution(
//                            ruleDefinition.getRuleId(),
//                            applicantId,
//                            null,
//                            true,
//                            ruleDefinition.getFraudPoints(),
//                            triggeredRule.getFlagDetails(),
//                            executionTime
//                    );
//                }
//            } catch (Exception e) {
//                log.error("Error executing rule {}: {}", ruleDefinition.getRuleCode(), e.getMessage());
//            }
//        }
//        
//        result.calculateRiskLevel();
//        
//        long totalTime = System.currentTimeMillis() - startTime;
//        log.info("Identity fraud detection completed for applicant {} in {}ms. Triggered {} rules.",
//                applicantId, totalTime, result.getTriggeredRules().size());
//        
//        return result;
//    }
//    
//    /**
//     * Execute a specific identity rule based on rule code
//     */
//    private FraudRule executeIdentityRule(FraudRuleDefinition ruleDefinition, 
//                                          Applicant applicant, 
//                                          ApplicantBasicDetails basicDetails) {
//        
//        String ruleCode = ruleDefinition.getRuleCode();
//        
//        return switch (ruleCode) {
//            case "DUPLICATE_AADHAAR" -> checkDuplicateAadhaar(ruleDefinition, applicant, basicDetails);
//            case "DUPLICATE_PAN" -> checkDuplicatePAN(ruleDefinition, applicant, basicDetails);
//            case "INVALID_PAN_FORMAT" -> checkInvalidPANFormat(ruleDefinition, basicDetails);
//            case "INVALID_AADHAAR_FORMAT" -> checkInvalidAadhaarFormat(ruleDefinition, basicDetails);
//            case "MINOR_APPLICANT" -> checkMinorApplicant(ruleDefinition, applicant);
//            case "SUSPICIOUS_AGE_HIGH" -> checkSuspiciousAgeHigh(ruleDefinition, applicant);
//            case "DUPLICATE_PHONE" -> checkDuplicatePhone(ruleDefinition, applicant);
//            case "DUPLICATE_EMAIL" -> checkDuplicateEmail(ruleDefinition, applicant);
//            default -> null;
//        };
//    }
//    
//    /**
//     * Check duplicate Aadhaar - fetches fraud points from database
//     */
//    private FraudRule checkDuplicateAadhaar(FraudRuleDefinition ruleDefinition,
//                                            Applicant applicant,
//                                            ApplicantBasicDetails basicDetails) {
//        if (basicDetails == null || basicDetails.getAadhaarNumber() == null) return null;
//        
//        List<ApplicantBasicDetails> duplicates = basicDetailsRepository.findAll().stream()
//                .filter(bd -> bd.getAadhaarNumber() != null &&
//                        bd.getAadhaarNumber().equals(basicDetails.getAadhaarNumber()) &&
//                        !bd.getApplicant().getApplicantId().equals(applicant.getApplicantId()))
//                .toList();
//        
//        if (!duplicates.isEmpty()) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Aadhaar number " + maskAadhaar(basicDetails.getAadhaarNumber()) +
//                            " is already used by " + duplicates.size() + " other applicant(s)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),     // FROM DATABASE
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Duplicate Aadhaar found in " + duplicates.size() + " application(s)"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check duplicate PAN - fetches fraud points from database
//     */
//    private FraudRule checkDuplicatePAN(FraudRuleDefinition ruleDefinition,
//                                        Applicant applicant,
//                                        ApplicantBasicDetails basicDetails) {
//        if (basicDetails == null || basicDetails.getPanNumber() == null) return null;
//        
//        List<ApplicantBasicDetails> duplicates = basicDetailsRepository.findAll().stream()
//                .filter(bd -> bd.getPanNumber() != null &&
//                        bd.getPanNumber().equals(basicDetails.getPanNumber()) &&
//                        !bd.getApplicant().getApplicantId().equals(applicant.getApplicantId()))
//                .toList();
//        
//        if (!duplicates.isEmpty()) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "PAN number " + basicDetails.getPanNumber() +
//                            " is already used by " + duplicates.size() + " other applicant(s)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),     // FROM DATABASE
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Duplicate PAN found in " + duplicates.size() + " application(s)"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check invalid PAN format - uses regex pattern from database
//     */
//    private FraudRule checkInvalidPANFormat(FraudRuleDefinition ruleDefinition,
//                                            ApplicantBasicDetails basicDetails) {
//        if (basicDetails == null || basicDetails.getPanNumber() == null) return null;
//        
//        // Get regex pattern from database
//        List<String> patterns = ruleDefinition.getPatternValues("REGEX");
//        if (patterns.isEmpty()) return null;
//        
//        Pattern panPattern = Pattern.compile(patterns.get(0));
//        
//        if (!panPattern.matcher(basicDetails.getPanNumber()).matches()) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "PAN number " + basicDetails.getPanNumber() + " does not match valid format",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Invalid PAN format detected"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check invalid Aadhaar format - uses regex pattern from database
//     */
//    private FraudRule checkInvalidAadhaarFormat(FraudRuleDefinition ruleDefinition,
//                                                ApplicantBasicDetails basicDetails) {
//        if (basicDetails == null || basicDetails.getAadhaarNumber() == null) return null;
//        
//        // Get regex pattern from database
//        List<String> patterns = ruleDefinition.getPatternValues("REGEX");
//        if (patterns.isEmpty()) return null;
//        
//        Pattern aadhaarPattern = Pattern.compile(patterns.get(0));
//        
//        if (!aadhaarPattern.matcher(basicDetails.getAadhaarNumber()).matches()) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Aadhaar number must be exactly 12 digits",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Invalid Aadhaar format"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check minor applicant - uses MIN_AGE parameter from database
//     */
//    private FraudRule checkMinorApplicant(FraudRuleDefinition ruleDefinition, Applicant applicant) {
//        if (applicant.getDob() == null) return null;
//        
//        // Get MIN_AGE parameter from database
//        Integer minAge = ruleDefinition.getParameterValueAsInt("MIN_AGE");
//        if (minAge == null) minAge = 18; // fallback default
//        
//        int age = Period.between(applicant.getDob(), LocalDate.now()).getYears();
//        
//        if (age < minAge) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Applicant is " + age + " years old (below " + minAge + " years)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Minor cannot apply for loan independently"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check suspicious age (high) - uses MAX_AGE parameter from database
//     */
//    private FraudRule checkSuspiciousAgeHigh(FraudRuleDefinition ruleDefinition, Applicant applicant) {
//        if (applicant.getDob() == null) return null;
//        
//        // Get MAX_AGE parameter from database
//        Integer maxAge = ruleDefinition.getParameterValueAsInt("MAX_AGE");
//        if (maxAge == null) maxAge = 80; // fallback default
//        
//        int age = Period.between(applicant.getDob(), LocalDate.now()).getYears();
//        
//        if (age > maxAge) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Applicant age is " + age + " years (above " + maxAge + ")",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Unusually high age for loan applicant"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check duplicate phone
//     */
//    private FraudRule checkDuplicatePhone(FraudRuleDefinition ruleDefinition, Applicant applicant) {
//        if (applicant.getPhone() == null) return null;
//        
//        List<Applicant> phoneMatches = applicantRepository.findAll().stream()
//                .filter(a -> a.getPhone() != null &&
//                        a.getPhone().equals(applicant.getPhone()) &&
//                        !a.getApplicantId().equals(applicant.getApplicantId()))
//                .toList();
//        
//        if (!phoneMatches.isEmpty()) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Phone number " + applicant.getPhone() + " is used by " +
//                            phoneMatches.size() + " other applicant(s)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Possible synthetic identity fraud - shared phone number"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check duplicate email
//     */
//    private FraudRule checkDuplicateEmail(FraudRuleDefinition ruleDefinition, Applicant applicant) {
//        if (applicant.getEmail() == null) return null;
//        
//        List<Applicant> emailMatches = applicantRepository.findAll().stream()
//                .filter(a -> a.getEmail() != null &&
//                        a.getEmail().equalsIgnoreCase(applicant.getEmail()) &&
//                        !a.getApplicantId().equals(applicant.getApplicantId()))
//                .toList();
//        
//        if (!emailMatches.isEmpty()) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Email " + applicant.getEmail() + " is used by " +
//                            emailMatches.size() + " other applicant(s)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Possible synthetic identity fraud - shared email"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Detect financial fraud using database rules
//     */
//    public FraudDetectionResult detectFinancialFraud(Long applicantId) {
//        Applicant applicant = applicantRepository.findById(applicantId)
//                .orElseThrow(() -> new RuntimeException("Applicant not found"));
//        
//        FraudDetectionResult result = new FraudDetectionResult();
//        result.setApplicantId(applicantId);
//        result.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
//        
//        // Get all active financial rules from database
//        Map<String, FraudRuleDefinition> rules = fraudRuleService.getRulesMapByCategory("FINANCIAL");
//        
//        // Get related data
//        ApplicantEmployment employment = employmentRepository
//                .findByApplicant_ApplicantId(applicantId).orElse(null);
//        ApplicantFinancials financials = financialsRepository
//                .findByApplicant_ApplicantId(applicantId).orElse(null);
//        ApplicantCreditHistory creditHistory = creditHistoryRepository
//                .findByApplicant_ApplicantId(applicantId).orElse(null);
//        
//        // Execute each rule
//        for (FraudRuleDefinition ruleDefinition : rules.values()) {
//            try {
//                FraudRule triggeredRule = executeFinancialRule(ruleDefinition, employment, financials, creditHistory);
//                
//                if (triggeredRule != null) {
//                    result.addTriggeredRule(triggeredRule);
//                }
//            } catch (Exception e) {
//                log.error("Error executing rule {}: {}", ruleDefinition.getRuleCode(), e.getMessage());
//            }
//        }
//        
//        result.calculateRiskLevel();
//        return result;
//    }
//    
//    /**
//     * Execute financial rule
//     */
//    private FraudRule executeFinancialRule(FraudRuleDefinition ruleDefinition,
//                                           ApplicantEmployment employment,
//                                           ApplicantFinancials financials,
//                                           ApplicantCreditHistory creditHistory) {
//        
//        String ruleCode = ruleDefinition.getRuleCode();
//        
//        return switch (ruleCode) {
//            case "HIGH_DEBT_TO_INCOME_RATIO" -> checkHighDTI(ruleDefinition, employment, creditHistory);
//            case "EXCESSIVE_CREDIT_UTILIZATION" -> checkCreditUtilization(ruleDefinition, creditHistory);
//            case "EXCESSIVE_ACTIVE_LOANS" -> checkActiveLoans(ruleDefinition, creditHistory);
//            default -> null;
//        };
//    }
//    
//    /**
//     * Check high DTI - uses MAX_DTI_RATIO parameter from database
//     */
//    private FraudRule checkHighDTI(FraudRuleDefinition ruleDefinition,
//                                    ApplicantEmployment employment,
//                                    ApplicantCreditHistory creditHistory) {
//        if (employment == null || creditHistory == null) return null;
//        if (employment.getMonthlyIncome() == null || creditHistory.getTotalMonthlyEmi() == null) return null;
//        
//        // Get MAX_DTI_RATIO from database
//        Double maxDTI = ruleDefinition.getParameterValueAsDouble("MAX_DTI_RATIO");
//        if (maxDTI == null) maxDTI = 50.0; // fallback
//        
//        BigDecimal monthlyIncome = employment.getMonthlyIncome();
//        BigDecimal totalEmi = creditHistory.getTotalMonthlyEmi();
//        
//        BigDecimal dtiRatio = totalEmi.divide(monthlyIncome, 4, java.math.RoundingMode.HALF_UP)
//                .multiply(new BigDecimal("100"));
//        
//        if (dtiRatio.compareTo(new BigDecimal(maxDTI)) > 0) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Debt-to-Income ratio is " + dtiRatio + "% (exceeds " + maxDTI + "%)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Monthly EMI ₹" + totalEmi + " consumes " + dtiRatio + "% of income ₹" + monthlyIncome
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check credit utilization - uses MAX_UTILIZATION parameter from database
//     */
//    private FraudRule checkCreditUtilization(FraudRuleDefinition ruleDefinition,
//                                             ApplicantCreditHistory creditHistory) {
//        if (creditHistory == null || creditHistory.getCreditUtilizationRatio() == null) return null;
//        
//        // Get MAX_UTILIZATION from database
//        Double maxUtilization = ruleDefinition.getParameterValueAsDouble("MAX_UTILIZATION");
//        if (maxUtilization == null) maxUtilization = 80.0; // fallback
//        
//        BigDecimal utilizationRatio = creditHistory.getCreditUtilizationRatio();
//        
//        if (utilizationRatio.compareTo(new BigDecimal(maxUtilization)) > 0) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Credit card utilization is " + utilizationRatio + "% (exceeds " + maxUtilization + "%)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "High credit utilization indicates financial stress"
//            );
//        }
//        return null;
//    }
//    
//    /**
//     * Check active loans - uses MAX_ACTIVE_LOANS parameter from database
//     */
//    private FraudRule checkActiveLoans(FraudRuleDefinition ruleDefinition,
//                                       ApplicantCreditHistory creditHistory) {
//        if (creditHistory == null || creditHistory.getTotalActiveLoans() == null) return null;
//        
//        // Get MAX_ACTIVE_LOANS from database
//        Integer maxLoans = ruleDefinition.getParameterValueAsInt("MAX_ACTIVE_LOANS");
//        if (maxLoans == null) maxLoans = 5; // fallback
//        
//        Integer activeLoans = creditHistory.getTotalActiveLoans();
//        
//        if (activeLoans >= maxLoans) {
//            return new FraudRule(
//                    ruleDefinition.getRuleCode(),
//                    "Applicant has " + activeLoans + " active loans (loan stacking fraud)",
//                    ruleDefinition.getFraudPoints(), // FROM DATABASE
//                    ruleDefinition.getSeverity(),
//                    ruleDefinition.getRuleCategory(),
//                    true,
//                    "Multiple active loans indicate loan stacking fraud"
//            );
//        }
//        return null;
//    }
//    
//    // Utility methods
//    private String maskAadhaar(String aadhaar) {
//        if (aadhaar == null || aadhaar.length() != 12) return aadhaar;
//        return "XXXX-XXXX-" + aadhaar.substring(8);
//    }
//}
