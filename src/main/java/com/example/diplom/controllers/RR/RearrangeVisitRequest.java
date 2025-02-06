package com.example.diplom.controllers.RR;

import java.time.LocalDateTime;
import java.util.UUID;

public record RearrangeVisitRequest(
        UUID visitId,
        LocalDateTime newVisitDate) {
}
