package com.example.diplom.controllers.RR;

import java.time.LocalDateTime;
import java.util.UUID;
//не менять, создавть новый если надо
public record VisitDateResponse(
        UUID visitId,
        LocalDateTime visitDate,
        String patientName,
        boolean isFinished) {
}
