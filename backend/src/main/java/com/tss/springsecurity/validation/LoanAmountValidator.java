package com.tss.springsecurity.validation;

import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class LoanAmountValidator implements ConstraintValidator<ValidLoanAmount, CompleteLoanApplicationDTO> {
    
    @Override
    public void initialize(ValidLoanAmount constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(CompleteLoanApplicationDTO dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getLoanAmount() == null || dto.getMonthlyIncome() == null) {
            return true; // Let other validators handle null checks
        }
        
        BigDecimal loanAmount = dto.getLoanAmount();
        BigDecimal monthlyIncome = dto.getMonthlyIncome();
        String loanType = dto.getLoanType();
        
        // Calculate maximum loan amount based on income (typically 60x monthly income)
        BigDecimal maxLoanBasedOnIncome = monthlyIncome.multiply(BigDecimal.valueOf(60));
        
        // Different loan types have different limits
        BigDecimal maxAllowedLoan;
        switch (loanType != null ? loanType.toLowerCase() : "") {
            case "personal":
                maxAllowedLoan = monthlyIncome.multiply(BigDecimal.valueOf(24)); // 2 years salary
                break;
            case "home":
                maxAllowedLoan = monthlyIncome.multiply(BigDecimal.valueOf(120)); // 10 years salary
                break;
            case "vehicle":
                maxAllowedLoan = monthlyIncome.multiply(BigDecimal.valueOf(48)); // 4 years salary
                break;
            case "education":
                maxAllowedLoan = monthlyIncome.multiply(BigDecimal.valueOf(72)); // 6 years salary
                break;
            case "business":
                maxAllowedLoan = monthlyIncome.multiply(BigDecimal.valueOf(100)); // 8+ years salary
                break;
            default:
                maxAllowedLoan = maxLoanBasedOnIncome;
        }
        
        if (loanAmount.compareTo(maxAllowedLoan) > 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Loan amount ₹%s exceeds maximum allowed ₹%s for %s loan based on monthly income ₹%s", 
                    loanAmount, maxAllowedLoan, loanType, monthlyIncome)
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
