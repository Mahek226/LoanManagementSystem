package com.tss.springsecurity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class AgeValidator implements ConstraintValidator<ValidAge, LocalDate> {
    
    private int minAge;
    private int maxAge;
    
    @Override
    public void initialize(ValidAge constraintAnnotation) {
        this.minAge = constraintAnnotation.min();
        this.maxAge = constraintAnnotation.max();
    }
    
    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null) {
            return true; // Let @NotNull handle null validation
        }
        
        LocalDate now = LocalDate.now();
        if (dateOfBirth.isAfter(now)) {
            return false; // Date of birth cannot be in the future
        }
        
        int age = Period.between(dateOfBirth, now).getYears();
        
        if (age < minAge) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Applicant must be at least %d years old", minAge)
            ).addConstraintViolation();
            return false;
        }
        
        if (age > maxAge) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Applicant cannot be older than %d years", maxAge)
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
