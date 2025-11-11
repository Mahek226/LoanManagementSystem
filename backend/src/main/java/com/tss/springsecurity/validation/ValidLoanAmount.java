package com.tss.springsecurity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LoanAmountValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLoanAmount {
    
    String message() default "Loan amount validation failed";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
