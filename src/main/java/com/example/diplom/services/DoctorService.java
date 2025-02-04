package com.example.diplom.services;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.services.dtos.VisitDto;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public interface DoctorService {
    void registerDoctor(DoctorRegisterRequest doctor);

    List<VisitDto> getDoctorVisitDates(UUID doctorId);

    void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest);
    CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest);
    List<ServiceResponse> getDoctorServices(UUID doctorId);
    List<PatientResponse> getDoctorPatients(UUID doctorId);

    void updateServicePrice(UUID doctorId, UpdateServiceRequest updateServiceRequest);
}
