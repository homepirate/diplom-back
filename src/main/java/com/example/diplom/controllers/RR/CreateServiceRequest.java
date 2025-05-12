package com.example.diplom.controllers.RR;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "Название не пустое")
        String name,
        BigDecimal price
) {
}
