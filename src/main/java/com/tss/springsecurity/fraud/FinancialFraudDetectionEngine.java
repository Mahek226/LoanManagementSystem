package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class FinancialFraudDetectionEngine {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantFinancialsRepository financialsRepository;
    private final ApplicantCreditHistoryRepository creditHistoryRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final OtherDocumentRepository otherDocumentRepository;
    
    public FinancialFraudDetectionEngine(
            ApplicantRepository applicantRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantFinancialsRepository financialsRepository,
            ApplicantCreditHistoryRepository creditHistoryRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            OtherDocumentRepository otherDocumentRepository) {
        this.applicantRepository = applicantRepository;
        this.employmentRepository = employmentRepository;
        this.financialsRepository = financialsRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.otherDocumentRepository = otherDocumentRepository;
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
        
        // Run all financial fraud rules
        checkLoanToIncomeRatio(employment, currentLoan, result);
        checkDebtToIncomeRatio(employment, creditHistory, result);
        checkSalaryMismatch(employment, financials, documents, result);
        checkLowBalanceHighLoan(financials, currentLoan, result);
        checkChequeBounces(financials, result);
        checkCashSalary(employment, result);
        checkUnfiledITR(employment, documents, result);
        checkITRSalaryMismatch(employment, documents, result);
        checkExcessiveCreditUtilization(creditHistory, result);
        checkMultipleActiveLoans(creditHistory, result);
        checkShortCreditHistory(applicant, creditHistory, documents, result);
        
        // Calculate final risk level
        result.calculateRiskLevel();
        
        return result;
    }
    
    /**
     * Rule 1: Loan-to-Income Ratio > 20
     * Fraud Points: +60 (HIGH)
     */
    private void checkLoanToIncomeRatio(ApplicantEmployment employment, 
                                        ApplicantLoanDetails loan,
                                        FraudDetectionResult result) {
        if (employment == null || loan == null) return;
        if (employment.getMonthlyIncome() == null || loan.getLoanAmount() == null) return;
        
        BigDecimal monthlyIncome = employment.getMonthlyIncome();
        BigDecimal annualIncome = monthlyIncome.multiply(new BigDecimal("12"));
        BigDecimal loanAmount = loan.getLoanAmount();
        
        // Calculate Loan-to-Income ratio
        BigDecimal loanToIncomeRatio = loanAmount.divide(annualIncome, 2, RoundingMode.HALF_UP);
        
        if (loanToIncomeRatio.compareTo(new BigDecimal("20")) > 0) {
            FraudRule rule = new FraudRule(
                "HIGH_LOAN_TO_INCOME_RATIO",
                "Loan-to-Income ratio is " + loanToIncomeRatio + " (exceeds 20x annual income)",
                60,
                "HIGH",
                "FINANCIAL",
                true,
                "Loan amount ₹" + loanAmount + " is too large for annual income ₹" + annualIncome
            );
            result.addTriggeredRule(rule);
        } else if (loanToIncomeRatio.compareTo(new BigDecimal("10")) > 0) {
            FraudRule rule = new FraudRule(
                "ELEVATED_LOAN_TO_INCOME_RATIO",
                "Loan-to-Income ratio is " + loanToIncomeRatio + " (exceeds 10x annual income)",
                30,
                "MEDIUM",
                "FINANCIAL",
                true,
                "Loan amount is significantly high compared to income"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 2: Debt-to-Income Ratio > 50%
     * Fraud Points: +50 (HIGH)
     */
    private void checkDebtToIncomeRatio(ApplicantEmployment employment,
                                        ApplicantCreditHistory creditHistory,
                                        FraudDetectionResult result) {
        if (employment == null || creditHistory == null) return;
        if (employment.getMonthlyIncome() == null || creditHistory.getTotalMonthlyEmi() == null) return;
        
        BigDecimal monthlyIncome = employment.getMonthlyIncome();
        BigDecimal totalEmi = creditHistory.getTotalMonthlyEmi();
        
        // Calculate Debt-to-Income ratio
        BigDecimal dtiRatio = totalEmi.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                                      .multiply(new BigDecimal("100"));
        
        if (dtiRatio.compareTo(new BigDecimal("50")) > 0) {
            FraudRule rule = new FraudRule(
                "HIGH_DEBT_TO_INCOME_RATIO",
                "Debt-to-Income ratio is " + dtiRatio + "% (exceeds 50%)",
                50,
                "HIGH",
                "FINANCIAL",
                true,
                "Monthly EMI ₹" + totalEmi + " consumes " + dtiRatio + "% of income ₹" + monthlyIncome
            );
            result.addTriggeredRule(rule);
        } else if (dtiRatio.compareTo(new BigDecimal("40")) > 0) {
            FraudRule rule = new FraudRule(
                "ELEVATED_DEBT_TO_INCOME_RATIO",
                "Debt-to-Income ratio is " + dtiRatio + "% (exceeds 40%)",
                30,
                "MEDIUM",
                "FINANCIAL",
                true,
                "High debt burden - " + dtiRatio + "% of income goes to EMI"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 3: Salary Mismatch - Payslip vs Bank Statement
     * Fraud Points: +55 (HIGH)
     */
    private void checkSalaryMismatch(ApplicantEmployment employment,
                                     ApplicantFinancials financials,
                                     List<OtherDocument> documents,
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
                    BigDecimal variance = declaredSalary.subtract(bankCredit).abs();
                    BigDecimal variancePercent = variance.divide(declaredSalary, 2, RoundingMode.HALF_UP)
                                                         .multiply(new BigDecimal("100"));
                    
                    FraudRule rule = new FraudRule(
                        "SALARY_MISMATCH",
                        "Declared salary ₹" + declaredSalary + " doesn't match bank credit ₹" + bankCredit,
                        55,
                        "HIGH",
                        "FINANCIAL",
                        true,
                        "Variance of " + variancePercent + "% between payslip and bank statement - Possible fake payslip"
                    );
                    result.addTriggeredRule(rule);
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
            FraudRule rule = new FraudRule(
                "LOW_BALANCE_HIGH_LOAN",
                "Very low average balance ₹" + avgBalance + " for loan request ₹" + loanAmount,
                45,
                "HIGH",
                "FINANCIAL",
                true,
                "Average balance is less than 1% of requested loan amount - Repayment capacity questionable"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 5: Frequent Cheque Bounces
     * Fraud Points: +40 (HIGH)
     */
    private void checkChequeBounces(ApplicantFinancials financials,
                                    FraudDetectionResult result) {
        if (financials == null || financials.getAnomalies() == null) return;
        
        String anomalies = financials.getAnomalies().toLowerCase();
        
        // Check for cheque bounce indicators
        if (anomalies.contains("cheque bounce") || 
            anomalies.contains("insufficient funds") ||
            anomalies.contains("payment failed") ||
            anomalies.contains("dishonoured")) {
            
            FraudRule rule = new FraudRule(
                "FREQUENT_CHEQUE_BOUNCES",
                "Bank statement shows cheque bounces or payment failures",
                40,
                "HIGH",
                "FINANCIAL",
                true,
                "Anomalies detected: " + financials.getAnomalies() + " - Poor repayment capacity"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 6: Cash Salary Declaration
     * Fraud Points: +35 (MEDIUM)
     */
    private void checkCashSalary(ApplicantEmployment employment,
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
            
            FraudRule rule = new FraudRule(
                "CASH_SALARY_DECLARATION",
                "Applicant declares cash-based salary or unverifiable income source",
                35,
                "MEDIUM",
                "FINANCIAL",
                true,
                "Employer: " + employerName + " - Income cannot be verified through bank statements"
            );
            result.addTriggeredRule(rule);
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
                    BigDecimal annualIncome = monthlyIncome.multiply(new BigDecimal("12"));
                    FraudRule rule = new FraudRule(
                        "UNFILED_ITR",
                        "Self-employed claiming high income (₹" + annualIncome + "/year) but no ITR filed",
                        50,
                        "HIGH",
                        "FINANCIAL",
                        true,
                        "No Income Tax Return found for self-employed applicant with annual income ₹" + annualIncome
                    );
                    result.addTriggeredRule(rule);
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
                        BigDecimal variance = declaredAnnualIncome.subtract(itrIncome).abs();
                        BigDecimal variancePercent = variance.divide(declaredAnnualIncome, 2, RoundingMode.HALF_UP)
                                                             .multiply(new BigDecimal("100"));
                        
                        FraudRule rule = new FraudRule(
                            "ITR_SALARY_MISMATCH",
                            "Declared annual income ₹" + declaredAnnualIncome + 
                            " doesn't match ITR income ₹" + itrIncome,
                            55,
                            "HIGH",
                            "FINANCIAL",
                            true,
                            "Variance of " + variancePercent + "% between declared income and ITR - Fraudulent income declaration"
                        );
                        result.addTriggeredRule(rule);
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
                                                 FraudDetectionResult result) {
        if (creditHistory == null || creditHistory.getCreditUtilizationRatio() == null) return;
        
        BigDecimal utilizationRatio = creditHistory.getCreditUtilizationRatio();
        
        if (utilizationRatio.compareTo(new BigDecimal("80")) > 0) {
            FraudRule rule = new FraudRule(
                "EXCESSIVE_CREDIT_UTILIZATION",
                "Credit card utilization is " + utilizationRatio + "% (exceeds 80%)",
                35,
                "MEDIUM",
                "FINANCIAL",
                true,
                "High credit utilization indicates financial stress and potential default risk"
            );
            result.addTriggeredRule(rule);
        } else if (utilizationRatio.compareTo(new BigDecimal("90")) > 0) {
            FraudRule rule = new FraudRule(
                "CRITICAL_CREDIT_UTILIZATION",
                "Credit card utilization is " + utilizationRatio + "% (exceeds 90%)",
                45,
                "HIGH",
                "FINANCIAL",
                true,
                "Critical credit utilization - Severe financial stress"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 10: Multiple Active Loans (Loan Stacking Fraud)
     * Fraud Points: +40 (HIGH)
     */
    private void checkMultipleActiveLoans(ApplicantCreditHistory creditHistory,
                                          FraudDetectionResult result) {
        if (creditHistory == null || creditHistory.getTotalActiveLoans() == null) return;
        
        Integer activeLoans = creditHistory.getTotalActiveLoans();
        
        if (activeLoans >= 5) {
            FraudRule rule = new FraudRule(
                "EXCESSIVE_ACTIVE_LOANS",
                "Applicant has " + activeLoans + " active loans (loan stacking fraud)",
                40,
                "HIGH",
                "FINANCIAL",
                true,
                "Multiple active loans indicate loan stacking fraud - Borrowing from multiple lenders simultaneously"
            );
            result.addTriggeredRule(rule);
        } else if (activeLoans >= 3) {
            FraudRule rule = new FraudRule(
                "MULTIPLE_ACTIVE_LOANS",
                "Applicant has " + activeLoans + " active loans",
                25,
                "MEDIUM",
                "FINANCIAL",
                true,
                "Multiple active loans increase default risk"
            );
            result.addTriggeredRule(rule);
        }
    }
    
    /**
     * Rule 11: Short Credit History
     * Fraud Points: +30 (MEDIUM)
     */
    private void checkShortCreditHistory(Applicant applicant,
                                         ApplicantCreditHistory creditHistory,
                                         List<OtherDocument> documents,
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
            FraudRule rule = new FraudRule(
                "SHORT_CREDIT_HISTORY",
                "Applicant (age " + age + ") has no credit history - No ITR, no credit cards, no loans",
                30,
                "MEDIUM",
                "FINANCIAL",
                true,
                "Thin file applicant - Unable to assess creditworthiness"
            );
            result.addTriggeredRule(rule);
        }
        
        // If age < 23 and requesting large loan
        if (age < 23 && !hasITR && !hasCreditCard) {
            FraudRule rule = new FraudRule(
                "NEW_TO_CREDIT",
                "Young applicant (age " + age + ") with no credit history",
                20,
                "LOW",
                "FINANCIAL",
                true,
                "New to credit - Higher risk profile"
            );
            result.addTriggeredRule(rule);
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
