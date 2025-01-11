package com.example.diplom.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final String PHONE_REGEX = "^8\\d{10}$";

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        // Инициализация, если необходимо
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null) {
            return false; // Или true, если поле может быть пустым
        }
        return phone.matches(PHONE_REGEX);
    }
}
