package com.example.diplom.utils.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Невалидный номер телефона. Длина 11 цифр и начинается с '8'.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
