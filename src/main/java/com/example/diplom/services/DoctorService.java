package com.example.diplom.services;

import com.example.diplom.controllers.RR.CreateServiceRequest;
import com.example.diplom.controllers.RR.CreateVisitRequest;
import com.example.diplom.controllers.RR.CreateVisitResponse;
import com.example.diplom.controllers.RR.DoctorRegisterRequest;
import com.example.diplom.services.dtos.VisitDto;

import java.util.List;
import java.util.UUID;

public interface DoctorService {
    void registerDoctor(DoctorRegisterRequest doctor);

    List<VisitDto> getDoctorVisitDates(UUID doctorId);

    void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest);
    CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest);

}
