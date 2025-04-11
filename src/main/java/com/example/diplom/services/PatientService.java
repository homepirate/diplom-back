package com.example.diplom.services;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.models.Patient;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    void registerPatient(PatientRegisterRequest patient);

    List<PatientVisitDetailsResponse> getVisitsByPatient(UUID patientId);

    List<DoctorResponse> getPatientDoctors(UUID patientId);

    PatientProfileResponse profileById(UUID patientId) ;

    void deleteAllPatientData(UUID patientId);

    void updatePatientProfile(UUID patientId, UpdatePatientProfileRequest updateRequest) ;

    void linkPatientWithDoctor(UUID patientId, String doctorCode);
}
