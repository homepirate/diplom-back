package com.example.diplom.controllers.RR;

import java.time.LocalDate;

public record AddPatientRequest(
        String fullName,
        String phone,
        LocalDate birthDate
) { }
