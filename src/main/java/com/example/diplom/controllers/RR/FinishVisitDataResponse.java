package com.example.diplom.controllers.RR;

import com.example.diplom.services.dtos.ServiceDTO;

import java.math.BigDecimal;
import java.util.List;

public record FinishVisitDataResponse(String notes, BigDecimal totalPrice, List<ServiceDTO> services) { }

