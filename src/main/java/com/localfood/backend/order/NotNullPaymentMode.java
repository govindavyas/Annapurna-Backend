package com.localfood.backend.order;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PaymentModeValidator.class)
public @interface NotNullPaymentMode {
    String message() default "paymentMode is required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
