package com.example.diplom.services.implementations;


import com.example.diplom.models.Patient;
import com.example.diplom.repositories.PatientRepository;
import com.example.diplom.services.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository, PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerPatient(Patient patient) {
        patient.setPassword(passwordEncoder.encode(patient.getPassword()));
        patient.setRole("ROLE_PATIENT");
        patientRepository.save(patient);
    }
}

