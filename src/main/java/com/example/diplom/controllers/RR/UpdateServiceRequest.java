package com.example.diplom.controllers.RR;

import java.math.BigDecimal;

public record UpdateServiceRequest(
        String name,
        BigDecimal price
) {}
