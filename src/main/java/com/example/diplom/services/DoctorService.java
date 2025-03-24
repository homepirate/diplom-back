package com.example.diplom.services;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.services.dtos.VisitDto;

import java.util.List;
import java.util.UUID;

public interface DoctorService {
    void registerDoctor(DoctorRegisterRequest doctor);

    List<VisitDto> getDoctorVisitDates(UUID doctorId, int month, int year);

    List<VisitDto> getDoctorVisitDatesByDay(UUID doctorId, String date);

    void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest);

    CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest);

    List<ServiceResponse> getDoctorServices(UUID doctorId);

    void updateServicePrice(UUID doctorId, UpdateServiceRequest updateServiceRequest);

    List<PatientResponse> getDoctorPatients(UUID doctorId);

    void rearrangeVisit(UUID doctorId, RearrangeVisitRequest rearrangeRequest);

    void cancelVisit(UUID doctorId, VisitIdRequest visitIdRequest);

    void finishVisit(UUID doctorId, FinishVisitRequest finishVisitRequest);

    VisitDetailsResponse getFinishVisitData(UUID doctorId, VisitIdRequest visitIdRequest);

    PatientMedCardResponse getPatientMedicalCard(UUID doctorId, UUID patientId);

    byte[] generateFinancialDashboardReport(UUID doctorId, ReportRequest reportRequest);
    PatientResponse addPatientManually(UUID doctorId, AddPatientRequest addPatientRequest);

}
