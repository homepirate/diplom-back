package com.example.diplom.controllers.RR;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VisitDetailsResponse(
        UUID visitId,
        LocalDateTime visitDate,
        boolean isFinished,
        String notes,
        BigDecimal totalPrice,
        List<VisitServicesDetailsResponse> services,
        List<String> attachmentUrls
) {}



