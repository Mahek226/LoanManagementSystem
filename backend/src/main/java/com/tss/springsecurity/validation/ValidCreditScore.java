package com.tss.springsecurity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CreditScoreValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCreditScore {
    
    String message() default "Credit score validation failed";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
