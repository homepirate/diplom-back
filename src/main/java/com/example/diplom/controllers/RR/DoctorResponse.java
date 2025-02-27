package com.example.diplom.controllers.RR;

import java.time.LocalDate;
import java.util.UUID;

public record DoctorResponse(
        String fullName,
        String specialization,
        UUID id){
}
