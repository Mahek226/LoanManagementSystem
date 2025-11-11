package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Service
public class FinancialFraudDetectionEngine {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantFinancialsRepository financialsRepository;
    private final ApplicantCreditHistoryRepository creditHistoryRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final OtherDocumentRepository otherDocumentRepository;
    private final DatabaseFraudRuleEngine dbRuleEngine;
    
    public FinancialFraudDetectionEngine(
            ApplicantRepository applicantRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantFinancialsRepository financialsRepository,
            ApplicantCreditHistoryRepository creditHistoryRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            OtherDocumentRepository otherDocumentRepository,
            DatabaseFraudRuleEngine dbRuleEngine) {
        this.applicantRepository = applicantRepository;
        this.employmentRepository = employmentRepository;
        this.financialsRepository = financialsRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.otherDocumentRepository = otherDocumentRepository;
        this.dbRuleEngine = dbRuleEngine;
    }
    
    /**
     * Run all financial fraud detection rules for an applicant
     */
    public FraudDetectionResult detectFinancialFraud(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        FraudDetectionResult result = new FraudDetectionResult();
        result.setApplicantId(applicantId);
        result.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        
        // Load active rules from database for FINANCIAL category
        Map<String, FraudRuleDefinition> rules = dbRuleEngine.getRulesAsMap("FINANCIAL");
        
        // Get related data
        ApplicantEmployment employment = employmentRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantFinancials financials = financialsRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        ApplicantCreditHistory creditHistory = creditHistoryRepository
                .findByApplicant_ApplicantId(applicantId).orElse(null);
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        List<OtherDocument> documents = otherDocumentRepository
                .findByApplicant_ApplicantId(applicantId);
        
        ApplicantLoanDetails currentLoan = loans.isEmpty() ? null : loans.get(loans.size() - 1);
        
        // Run all financial fraud rules (only if enabled in database)
        checkLoanToIncomeRatio(employment, currentLoan, rules, result);
        checkDebtToIncomeRatio(employment, creditHistory, rules, result);
        checkSalaryMismatch(employment, financials, documents, rules, result);
        checkLowBalanceHighLoan(financials, currentLoan, rules, result);
        checkChequeBounces(financials, rules, result);
        checkCashSalary(employment, rules, result);
        checkUnfiledITR(employment, documents, rules, result);
        checkITRSalaryMismatch(employment, documents, rules, result);
        checkExcessiveCreditUtilization(creditHistory, rules, result);
        checkMultipleActiveLoans(creditHistory, rules, result);
        checkShortCreditHistory(applicant, creditHistory, documents, rules, result);
        
        // Calculate final risk level
        result.calculateRiskLevel();
        
        return result;
    }
    
    /**
     * Rule 1: Loan-to-Income Ratio > 20
     */
    private void checkLoanToIncomeRatio(ApplicantEmployment employment, 
                                        ApplicantLoanDetails loan,
                                        Map<String, FraudRuleDefinition> rules,
                                        FraudDetectionResult result) {
        if (employment == null || loan == null) return;
        if (employment.getMonthlyIncome() == null || loan.getLoanAmount() == null) return;
        
        BigDecimal monthlyIncome = employment.getMonthlyIncome();
        BigDecimal annualIncome = monthlyIncome.multiply(new BigDecimal("12"));
        BigDecimal loanAmount = loan.getLoanAmount();
        
        // Calculate Loan-to-Income ratio
        BigDecimal loanToIncomeRatio = loanAmount.divide(annualIncome, 2, RoundingMode.HALF_UP);
        
        if (loanToIncomeRatio.compareTo(new BigDecimal("20")) > 0) {
            FraudRuleDefinition ruleDef = rules.get("HIGH_LOAN_TO_INCOME_RATIO");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Loan-to-Income ratio is " + loanToIncomeRatio + " (exceeds 20x annual income)";
                String flagDetails = "Loan amount ₹" + loanAmount + " is too large for annual income ₹" + annualIncome;
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        } else if (loanToIncomeRatio.compareTo(new BigDecimal("10")) > 0) {
            FraudRuleDefinition ruleDef = rules.get("ELEVATED_LOAN_TO_INCOME_RATIO");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Loan-to-Income ratio is " + loanToIncomeRatio + " (exceeds 10x annual income)";
                String flagDetails = "Loan amount is significantly high compared to income";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 2: Debt-to-Income Ratio > 50%
     */
    private void checkDebtToIncomeRatio(ApplicantEmployment employment,
                                        ApplicantCreditHistory creditHistory,
                                        Map<String, FraudRuleDefinition> rules,
                                        FraudDetectionResult result) {
        if (employment == null || creditHistory == null) return;
        if (employment.getMonthlyIncome() == null || creditHistory.getTotalMonthlyEmi() == null) return;
        
        BigDecimal monthlyIncome = employment.getMonthlyIncome();
        BigDecimal totalEmi = creditHistory.getTotalMonthlyEmi();
        
        // Calculate Debt-to-Income ratio
        BigDecimal dtiRatio = totalEmi.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                                      .multiply(new BigDecimal("100"));
        
        if (dtiRatio.compareTo(new BigDecimal("50")) > 0) {
            FraudRuleDefinition ruleDef = rules.get("HIGH_DEBT_TO_INCOME_RATIO");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Debt-to-Income ratio is " + dtiRatio + "% (exceeds 50%)";
                String flagDetails = "Monthly EMI ₹" + totalEmi + " consumes " + dtiRatio + "% of income ₹" + monthlyIncome;
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        } else if (dtiRatio.compareTo(new BigDecimal("40")) > 0) {
            FraudRuleDefinition ruleDef = rules.get("ELEVATED_DEBT_TO_INCOME_RATIO");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Debt-to-Income ratio is " + dtiRatio + "% (exceeds 40%)";
                String flagDetails = "High debt burden - " + dtiRatio + "% of income goes to EMI";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 3: Salary Mismatch - Payslip vs Bank Statement
     * Fraud Points: +55 (HIGH)
     */
    private void checkSalaryMismatch(ApplicantEmployment employment,
                                     ApplicantFinancials financials,
                                     List<OtherDocument> documents,
                                     Map<String, FraudRuleDefinition> rules,
                                     FraudDetectionResult result) {
        if (employment == null || financials == null) return;
        if (employment.getMonthlyIncome() == null) return;
        
        BigDecimal declaredSalary = employment.getMonthlyIncome();
        
        // Check if bank statement shows different credit amount
        if (financials.getTotalCreditLastMonth() != null) {
            BigDecimal bankCredit = financials.getTotalCreditLastMonth();
            
            // Allow 20% variance for deductions, bonuses, etc.
            BigDecimal lowerBound = declaredSalary.multiply(new BigDecimal("0.70"));
            BigDecimal upperBound = declaredSalary.multiply(new BigDecimal("1.30"));
            
            if (bankCredit.compareTo(lowerBound) < 0 || bankCredit.compareTo(upperBound) > 0) {
                // Check if payslip document exists
                boolean hasPayslip = documents.stream()
                        .anyMatch(doc -> "payslip".equalsIgnoreCase(doc.getDocType()));
                
                if (hasPayslip) {
                    FraudRuleDefinition ruleDef = rules.get("SALARY_MISMATCH");
                    if (ruleDef != null && ruleDef.getIsActive()) {
                        BigDecimal variance = declaredSalary.subtract(bankCredit).abs();
                        BigDecimal variancePercent = variance.divide(declaredSalary, 2, RoundingMode.HALF_UP)
                                                             .multiply(new BigDecimal("100"));
                        
                        String customDesc = "Declared salary ₹" + declaredSalary + " doesn't match bank credit ₹" + bankCredit;
                        String flagDetails = "Variance of " + variancePercent + "% between payslip and bank statement - Possible fake payslip";
                        FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                        result.addTriggeredRule(rule);
                    }
                }
            }
        }
    }
    
    /**
     * Rule 4: Very Low Average Balance vs High Loan
     * Fraud Points: +45 (HIGH)
     */
    private void checkLowBalanceHighLoan(ApplicantFinancials financials,
                                         ApplicantLoanDetails loan,
                                         Map<String, FraudRuleDefinition> rules,
                                         FraudDetectionResult result) {
        if (financials == null || loan == null) return;
        if (loan.getLoanAmount() == null) return;
        
        // Calculate average balance from last month transactions
        BigDecimal avgBalance = BigDecimal.ZERO;
        if (financials.getTotalCreditLastMonth() != null && financials.getTotalDebitLastMonth() != null) {
            BigDecimal credit = financials.getTotalCreditLastMonth();
            BigDecimal debit = financials.getTotalDebitLastMonth();
            avgBalance = credit.subtract(debit).abs();
        }
        
        BigDecimal loanAmount = loan.getLoanAmount();
        
        // If average balance is less than 1% of loan amount
        BigDecimal threshold = loanAmount.multiply(new BigDecimal("0.01"));
        
        if (avgBalance.compareTo(threshold) < 0 && avgBalance.compareTo(new BigDecimal("10000")) < 0) {
            FraudRuleDefinition ruleDef = rules.get("LOW_BALANCE_HIGH_LOAN");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Very low average balance ₹" + avgBalance + " for loan request ₹" + loanAmount;
                String flagDetails = "Average balance is less than 1% of requested loan amount - Repayment capacity questionable";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 5: Frequent Cheque Bounces
     * Fraud Points: +40 (HIGH)
     */
    private void checkChequeBounces(ApplicantFinancials financials,
                                    Map<String, FraudRuleDefinition> rules,
                                    FraudDetectionResult result) {
        if (financials == null || financials.getAnomalies() == null) return;
        
        String anomalies = financials.getAnomalies().toLowerCase();
        
        // Check for cheque bounce indicators
        if (anomalies.contains("cheque bounce") || 
            anomalies.contains("insufficient funds") ||
            anomalies.contains("payment failed") ||
            anomalies.contains("dishonoured")) {
            
            FraudRuleDefinition ruleDef = rules.get("FREQUENT_CHEQUE_BOUNCES");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Anomalies detected: " + financials.getAnomalies() + " - Poor repayment capacity";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 6: Cash Salary Declaration
     * Fraud Points: +35 (MEDIUM)
     */
    private void checkCashSalary(ApplicantEmployment employment,
                                 Map<String, FraudRuleDefinition> rules,
                                 FraudDetectionResult result) {
        if (employment == null) return;
        
        // Check if employment type suggests cash payment
        String employmentType = employment.getEmploymentType();
        String employerName = employment.getEmployerName();
        
        if (employerName != null && (
            employerName.toLowerCase().contains("cash") ||
            employerName.toLowerCase().contains("daily wage") ||
            employerName.toLowerCase().contains("contract") ||
            employerName.toLowerCase().contains("freelance"))) {
            
            FraudRuleDefinition ruleDef = rules.get("CASH_SALARY_DECLARATION");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String flagDetails = "Employer: " + employerName + " - Income cannot be verified through bank statements";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        // Self-employed with no verifiable income trail
        if ("self-employed".equalsIgnoreCase(employmentType)) {
            // This will be checked in ITR rules
        }
    }
    
    /**
     * Rule 7: Unfiled ITR (Self-employed with no tax record)
     * Fraud Points: +50 (HIGH)
     */
    private void checkUnfiledITR(ApplicantEmployment employment,
                                 List<OtherDocument> documents,
                                 Map<String, FraudRuleDefinition> rules,
                                 FraudDetectionResult result) {
        if (employment == null) return;
        
        String employmentType = employment.getEmploymentType();
        BigDecimal monthlyIncome = employment.getMonthlyIncome();
        
        // Check if self-employed with high income
        if ("self-employed".equalsIgnoreCase(employmentType)) {
            if (monthlyIncome != null && monthlyIncome.compareTo(new BigDecimal("50000")) > 0) {
                // Check if ITR document exists
                boolean hasITR = documents.stream()
                        .anyMatch(doc -> "itr".equalsIgnoreCase(doc.getDocType()));
                
                if (!hasITR) {
                    FraudRuleDefinition ruleDef = rules.get("UNFILED_ITR");
                    if (ruleDef != null && ruleDef.getIsActive()) {
                        BigDecimal annualIncome = monthlyIncome.multiply(new BigDecimal("12"));
                        String customDesc = "Self-employed claiming high income (₹" + annualIncome + "/year) but no ITR filed";
                        String flagDetails = "No Income Tax Return found for self-employed applicant with annual income ₹" + annualIncome;
                        FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                        result.addTriggeredRule(rule);
                    }
                }
            }
        }
    }
    
    /**
     * Rule 8: ITR and Declared Salary Mismatch
     * Fraud Points: +55 (HIGH)
     */
    private void checkITRSalaryMismatch(ApplicantEmployment employment,
                                        List<OtherDocument> documents,
                                        Map<String, FraudRuleDefinition> rules,
                                        FraudDetectionResult result) {
        if (employment == null || employment.getMonthlyIncome() == null) return;
        
        BigDecimal declaredMonthlyIncome = employment.getMonthlyIncome();
        BigDecimal declaredAnnualIncome = declaredMonthlyIncome.multiply(new BigDecimal("12"));
        
        // Find ITR document
        for (OtherDocument doc : documents) {
            if ("itr".equalsIgnoreCase(doc.getDocType()) && doc.getOcrText() != null) {
                String ocrText = doc.getOcrText().toLowerCase();
                
                // Try to extract income from OCR text
                BigDecimal itrIncome = extractIncomeFromOCR(ocrText);
                
                if (itrIncome != null && itrIncome.compareTo(BigDecimal.ZERO) > 0) {
                    // Allow 30% variance for deductions, exemptions
                    BigDecimal lowerBound = declaredAnnualIncome.multiply(new BigDecimal("0.60"));
                    BigDecimal upperBound = declaredAnnualIncome.multiply(new BigDecimal("1.40"));
                    
                    if (itrIncome.compareTo(lowerBound) < 0 || itrIncome.compareTo(upperBound) > 0) {
                        FraudRuleDefinition ruleDef = rules.get("ITR_SALARY_MISMATCH");
                        if (ruleDef != null && ruleDef.getIsActive()) {
                            BigDecimal variance = declaredAnnualIncome.subtract(itrIncome).abs();
                            BigDecimal variancePercent = variance.divide(declaredAnnualIncome, 2, RoundingMode.HALF_UP)
                                                                 .multiply(new BigDecimal("100"));
                            
                            String customDesc = "Declared annual income ₹" + declaredAnnualIncome + 
                                " doesn't match ITR income ₹" + itrIncome;
                            String flagDetails = "Variance of " + variancePercent + "% between declared income and ITR - Fraudulent income declaration";
                            FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                            result.addTriggeredRule(rule);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Rule 9: Excessive Credit Card Utilization (>80%)
     * Fraud Points: +35 (MEDIUM)
     */
    private void checkExcessiveCreditUtilization(ApplicantCreditHistory creditHistory,
                                                 Map<String, FraudRuleDefinition> rules,
                                                 FraudDetectionResult result) {
        if (creditHistory == null || creditHistory.getCreditUtilizationRatio() == null) return;
        
        BigDecimal utilizationRatio = creditHistory.getCreditUtilizationRatio();
        
        if (utilizationRatio.compareTo(new BigDecimal("90")) > 0) {
            FraudRuleDefinition ruleDef = rules.get("CRITICAL_CREDIT_UTILIZATION");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Credit card utilization is " + utilizationRatio + "% (exceeds 90%)";
                String flagDetails = "Critical credit utilization - Severe financial stress";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        } else if (utilizationRatio.compareTo(new BigDecimal("80")) > 0) {
            FraudRuleDefinition ruleDef = rules.get("EXCESSIVE_CREDIT_UTILIZATION");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Credit card utilization is " + utilizationRatio + "% (exceeds 80%)";
                String flagDetails = "High credit utilization indicates financial stress and potential default risk";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 10: Multiple Active Loans (Loan Stacking Fraud)
     * Fraud Points: +40 (HIGH)
     */
    private void checkMultipleActiveLoans(ApplicantCreditHistory creditHistory,
                                          Map<String, FraudRuleDefinition> rules,
                                          FraudDetectionResult result) {
        if (creditHistory == null || creditHistory.getTotalActiveLoans() == null) return;
        
        Integer activeLoans = creditHistory.getTotalActiveLoans();
        
        if (activeLoans >= 5) {
            FraudRuleDefinition ruleDef = rules.get("EXCESSIVE_ACTIVE_LOANS");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Applicant has " + activeLoans + " active loans (loan stacking fraud)";
                String flagDetails = "Multiple active loans indicate loan stacking fraud - Borrowing from multiple lenders simultaneously";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        } else if (activeLoans >= 3) {
            FraudRuleDefinition ruleDef = rules.get("MULTIPLE_ACTIVE_LOANS");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Applicant has " + activeLoans + " active loans";
                String flagDetails = "Multiple active loans increase default risk";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    /**
     * Rule 11: Short Credit History
     * Fraud Points: +30 (MEDIUM)
     */
    private void checkShortCreditHistory(Applicant applicant,
                                         ApplicantCreditHistory creditHistory,
                                         List<OtherDocument> documents,
                                         Map<String, FraudRuleDefinition> rules,
                                         FraudDetectionResult result) {
        if (applicant.getDob() == null) return;
        
        int age = Period.between(applicant.getDob(), LocalDate.now()).getYears();
        
        // Check if new applicant with no credit history
        boolean hasITR = documents.stream().anyMatch(doc -> "itr".equalsIgnoreCase(doc.getDocType()));
        boolean hasCreditCard = creditHistory != null && 
                               creditHistory.getCreditCardCount() != null && 
                               creditHistory.getCreditCardCount() > 0;
        boolean hasActiveLoans = creditHistory != null && 
                                creditHistory.getTotalActiveLoans() != null && 
                                creditHistory.getTotalActiveLoans() > 0;
        
        // If age > 25 but no credit history
        if (age > 25 && !hasITR && !hasCreditCard && !hasActiveLoans) {
            FraudRuleDefinition ruleDef = rules.get("SHORT_CREDIT_HISTORY");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Applicant (age " + age + ") has no credit history - No ITR, no credit cards, no loans";
                String flagDetails = "Thin file applicant - Unable to assess creditworthiness";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
        
        // If age < 23 and requesting large loan
        if (age < 23 && !hasITR && !hasCreditCard) {
            FraudRuleDefinition ruleDef = rules.get("NEW_TO_CREDIT");
            if (ruleDef != null && ruleDef.getIsActive()) {
                String customDesc = "Young applicant (age " + age + ") with no credit history";
                String flagDetails = "New to credit - Higher risk profile";
                FraudRule rule = dbRuleEngine.createFraudRule(ruleDef, customDesc, flagDetails);
                result.addTriggeredRule(rule);
            }
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Extract income amount from ITR OCR text
     */
    private BigDecimal extractIncomeFromOCR(String ocrText) {
        try {
            // Look for patterns like "Total Income: 1440000" or "Gross Income: 14,40,000"
            String[] patterns = {
                "total income[:\\s]+([0-9,]+)",
                "gross income[:\\s]+([0-9,]+)",
                "taxable income[:\\s]+([0-9,]+)",
                "income[:\\s]+([0-9,]+)"
            };
            
            for (String pattern : patterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(ocrText);
                if (m.find()) {
                    String amountStr = m.group(1).replaceAll(",", "");
                    return new BigDecimal(amountStr);
                }
            }
        } catch (Exception e) {
            // Unable to extract income
        }
        return null;
    }
}
