package com.example.diplom.controllers.RR;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientProfileResponse(
        String fullName,
        LocalDate birthDate,
        String email,
        String phone,
        List<String> attachments
) {
}