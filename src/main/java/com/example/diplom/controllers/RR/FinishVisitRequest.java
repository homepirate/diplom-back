package com.example.diplom.controllers.RR;

import java.util.List;
import java.util.UUID;

public record FinishVisitRequest(
        UUID id,
        List<ServiceUpdateRequest> services,
        String notes) { }
