package com.example.diplom.services;

import com.example.diplom.controllers.RR.DoctorResponse;
import com.example.diplom.controllers.RR.PatientRegisterRequest;
import com.example.diplom.controllers.RR.PatientVisitDetailsResponse;
import com.example.diplom.models.Patient;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    void registerPatient(PatientRegisterRequest patient);

        List<PatientVisitDetailsResponse> getVisitsByPatient(UUID patientId);

    List<DoctorResponse> getPatientDoctors(UUID patientId);
}
