package com.example.diplom.controllers.RR;

import com.example.diplom.utils.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PatientRegisterRequest(
        @NotBlank
        @Email(message = "Невалидный email")
        String email,

        @NotBlank(message = "Пароль не должен быть пустым")
        String password,

        @ValidPhone
        String phone,

        @NotBlank(message = "Имя не должно быть пустым")
        String fullName,

        @NotNull
        @Past(message = "В прошлом надо рождаться")
        LocalDate birthDate
) {
}
