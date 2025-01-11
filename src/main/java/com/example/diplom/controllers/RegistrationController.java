package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.DoctorRegisterRequest;
import com.example.diplom.controllers.RR.PatientRegisterRequest;
import com.example.diplom.controllers.RR.RegResponse;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Patient;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<RegResponse> registerDoctor(@Valid @RequestBody DoctorRegisterRequest doctor) {
        doctorService.registerDoctor(doctor);
        RegResponse response = new RegResponse("CREATED", "Doctor registered successfully!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/patient")
    public ResponseEntity<RegResponse> registerPatient(@Valid @RequestBody PatientRegisterRequest patient) {
        patientService.registerPatient(patient);
        RegResponse response = new RegResponse("CREATED", "Patient registered successfully!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
