package com.example.diplom.controllers.RR;


import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVisitRequest(UUID patientId, LocalDateTime visitDate, String notes, boolean force) { }
