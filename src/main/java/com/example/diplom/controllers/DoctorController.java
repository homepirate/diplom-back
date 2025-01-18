package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.DoctorResponse;
import com.example.diplom.controllers.RR.VisitDateResponse;
import com.example.diplom.controllers.interfaces.DoctorAPI;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.VisitDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class DoctorController implements DoctorAPI {
    private DoctorService doctorService;

    @Autowired
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public DoctorResponse getAllDoctors(int page) {
        return null;
    }

    @Override
    public List<VisitDateResponse> getDoctorVisitDates(UUID doctorId) {
       List<VisitDto> visits = doctorService.getDoctorVisitDates(doctorId);

        return visits.stream()
                .map(visit -> new VisitDateResponse(visit.getId(), visit.getVisitDate()))
                .collect(Collectors.toList());
    }
}
