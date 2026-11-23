package com.lab.projetosanquim.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DataNaoFuturaValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataNaoFutura {
    String message() default "A data da chamada não pode ser futura";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
