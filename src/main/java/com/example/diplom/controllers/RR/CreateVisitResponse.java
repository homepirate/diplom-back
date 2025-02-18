package com.example.diplom.controllers.RR;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVisitResponse(
        LocalDateTime visitDate,
        UUID visitId
) {
}
