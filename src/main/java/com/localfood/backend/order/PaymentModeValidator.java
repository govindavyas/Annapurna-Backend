package com.localfood.backend.order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PaymentModeValidator implements ConstraintValidator<NotNullPaymentMode, PaymentMode> {

    @Override
    public boolean isValid(PaymentMode value, ConstraintValidatorContext context) {
        return value != null;
    }
}
