package com.example.diplom.services;

import com.example.diplom.controllers.RR.PatientRegisterRequest;
import com.example.diplom.models.Patient;

public interface PatientService {
    void registerPatient(PatientRegisterRequest patient);
}
