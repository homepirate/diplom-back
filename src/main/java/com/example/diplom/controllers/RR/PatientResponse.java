package com.example.diplom.controllers.RR;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        String fullName,
        LocalDate birthDate,
        UUID id
) {}
