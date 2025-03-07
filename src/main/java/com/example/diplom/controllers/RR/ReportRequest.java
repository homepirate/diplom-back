package com.example.diplom.controllers.RR;

import java.time.LocalDateTime;

public record ReportRequest(LocalDateTime startDate,
                            LocalDateTime endDate) {
}
