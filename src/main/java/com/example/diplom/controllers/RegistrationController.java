package com.example.diplom.controllers;

import com.example.diplom.models.Doctor;
import com.example.diplom.models.Patient;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegistrationController {

    private final DoctorService doctorService;
    private final PatientService patientService;

    @Autowired
    public RegistrationController(DoctorService doctorService, PatientService patientService) {
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    @PostMapping("/doctor")
    public String registerDoctor(@RequestBody Doctor doctor) {
        doctorService.registerDoctor(doctor);
        return "Doctor registered successfully!";
    }

    @PostMapping("/patient")
    public String registerPatient(@RequestBody Patient patient) {
        patientService.registerPatient(patient);
        return "Patient registered successfully!";
    }
}
