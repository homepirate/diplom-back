package com.example.diplom.controllers.RR;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record ReportRequest(@JsonFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
                            @JsonFormat(pattern = "dd-MM-yyyy") LocalDate endDate) { }
