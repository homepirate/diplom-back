package com.example.diplom.controllers.RR;

import java.time.LocalDate;

public record UpdatePatientProfileRequest(
        String fullName,
        LocalDate birthDate,
        String email,
        String phone
) {
}
