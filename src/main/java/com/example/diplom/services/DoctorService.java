package com.example.diplom.services;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.services.dtos.VisitDto;

import java.util.List;
import java.util.UUID;

public interface DoctorService {
    void registerDoctor(DoctorRegisterRequest doctor);

    List<VisitDto> getDoctorVisitDates(UUID doctorId);

    void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest);

    CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest);

    List<ServiceResponse> getDoctorServices(UUID doctorId);

    List<PatientResponse> getDoctorPatients(UUID doctorId);

    void updateServicePrice(UUID doctorId, UpdateServiceRequest updateServiceRequest);

    void rearrangeVisit(UUID doctorId, RearrangeVisitRequest rearrangeRequest);

    void cancelVisit(VisitIdRequest visitIdRequest);

    VisitNotesResponse getVisitDescription(VisitIdRequest visitIdRequest);

    void finishVisit(FinishVisitRequest finishVisitRequest);

    VisitDetailsResponse getFinishVisitData(VisitIdRequest visitIdRequest);

    PatientMedCardResponse getPatientMedicalCard(UUID doctorId, UUID patientId);


}
