package com.example.diplom.controllers.RR;

import java.time.LocalDateTime;

public record CreateVisitResponse(
        LocalDateTime visitDate // Дата и время визита
) {
}
