package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.PatientRegisterRequest;
import com.example.diplom.models.Patient;
import com.example.diplom.repositories.PatientRepository;
import com.example.diplom.services.PatientService;
import com.example.diplom.services.dtos.PatientRegistrationDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Override
    public void registerPatient(PatientRegisterRequest patient) {
        PatientRegistrationDto patientDto = new PatientRegistrationDto(patient.password(),null, patient.email(), patient.phone(),
                patient.fullName(), patient.birthDate());
        patientDto.setPassword(passwordEncoder.encode(patientDto.getPassword()));
        patientDto.setRole("ROLE_PATIENT");

        patientRepository.save(modelMapper.map(patientDto, Patient.class));
    }
}

