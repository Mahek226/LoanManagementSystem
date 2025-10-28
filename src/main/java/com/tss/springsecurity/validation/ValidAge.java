package com.tss.springsecurity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AgeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAge {
    
    String message() default "Invalid age";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    int min() default 18;
    
    int max() default 100;
}
