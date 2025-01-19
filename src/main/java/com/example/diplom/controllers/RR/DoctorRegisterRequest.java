package com.example.diplom.controllers.RR;

import com.example.diplom.utils.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record DoctorRegisterRequest(
        @NotBlank
        @Email(message = "Невалидный email")
        String email,

        @NotBlank(message = "Пароль не должен быть пустым")
        @Length(min = 6)
        String password,

        @ValidPhone
        String phone,

        @NotBlank(message = "Имя не должно быть пустым")
        String fullName,

        @NotBlank(message = "Специализация не должна быть пустой")
        String specialization
) {
}
