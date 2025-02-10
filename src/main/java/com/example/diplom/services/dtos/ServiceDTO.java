package com.example.diplom.services.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceDTO(UUID serviceId, String name, BigDecimal price, int quantity) { }

