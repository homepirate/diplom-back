package com.example.diplom.controllers.RR;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdatePatientProfileRequest(
        String fullName,
        LocalDate birthDate,
        String email,
        String phone
) {
}
