package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.CreateServiceRequest;
import com.example.diplom.controllers.RR.CreateServiceResponse;
import com.example.diplom.controllers.RR.DoctorResponse;
import com.example.diplom.controllers.RR.VisitDateResponse;
import com.example.diplom.controllers.interfaces.DoctorAPI;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.VisitDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


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
    public List<VisitDateResponse> getDoctorVisitDates() {

        List<VisitDto> visits = doctorService.getDoctorVisitDates(getDoctorId());
        return visits.stream()
                .map(visit -> new VisitDateResponse(visit.getId(), visit.getVisitDate()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<?>  createService(CreateServiceRequest serviceRequest) {
        UUID doctorId = getDoctorId();
        doctorService.createServiceForDoctor(doctorId, serviceRequest);
        CreateServiceResponse createServiceResponse = new CreateServiceResponse("CREATED", "Service created");
        return ResponseEntity.status(HttpStatus.CREATED).body(createServiceResponse);

    }

    UUID getDoctorId(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID doctorId = UUID.fromString(jwt.getClaim("id"));
        return doctorId;
    }
}