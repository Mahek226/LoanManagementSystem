package com.tss.springsecurity.validation;

import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class CreditScoreValidator implements ConstraintValidator<ValidCreditScore, CompleteLoanApplicationDTO> {
    
    @Override
    public void initialize(ValidCreditScore constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(CompleteLoanApplicationDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        
        Integer creditScore = dto.getCreditScore();
        String loanType = dto.getLoanType();
        BigDecimal loanAmount = dto.getLoanAmount();
        
        if (creditScore == null || loanType == null || loanAmount == null) {
            return true; // Let other validators handle null checks
        }
        
        // Define minimum credit score requirements for different loan types
        int minRequiredScore;
        switch (loanType.toLowerCase()) {
            case "personal":
                minRequiredScore = loanAmount.compareTo(BigDecimal.valueOf(500000)) > 0 ? 700 : 650;
                break;
            case "home":
                minRequiredScore = loanAmount.compareTo(BigDecimal.valueOf(2000000)) > 0 ? 750 : 700;
                break;
            case "vehicle":
                minRequiredScore = 600;
                break;
            case "education":
                minRequiredScore = 650;
                break;
            case "business":
                minRequiredScore = 720;
                break;
            case "gold":
                minRequiredScore = 550; // Gold loans are secured, so lower requirement
                break;
            default:
                minRequiredScore = 650;
        }
        
        if (creditScore < minRequiredScore) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Credit score %d is below minimum required score %d for %s loan of amount ₹%s", 
                    creditScore, minRequiredScore, loanType, loanAmount)
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
