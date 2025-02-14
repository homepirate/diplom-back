package com.example.diplom.controllers.RR;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateVisitRequest(
        UUID patientId,         // ID пациента
        LocalDateTime visitDate, // Дата и время визита
        String notes
) {
}