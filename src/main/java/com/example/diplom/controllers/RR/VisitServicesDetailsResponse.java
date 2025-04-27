package com.example.diplom.controllers.RR;


import java.math.BigDecimal;
import java.util.UUID;

public record VisitServicesDetailsResponse
        (
                UUID serviceId,
                String name,
                BigDecimal price,
                int quantity
        ) {
}

