package com.example.diplom.controllers.RR;

import java.time.LocalDate;

public record PatientResponse(
        String fullName,
        LocalDate birthDate
) {}
