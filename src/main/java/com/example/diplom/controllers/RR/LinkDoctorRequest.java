package com.example.diplom.controllers.RR;

import jakarta.validation.constraints.NotBlank;

public record LinkDoctorRequest(@NotBlank String doctorCode) {}
