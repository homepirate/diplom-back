package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.DoctorRegisterRequest;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Specialization;
import com.example.diplom.repositories.DoctorRepository;
import com.example.diplom.repositories.SpecializationRepository;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.DoctorRegistrationDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpecializationRepository specializationRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public DoctorServiceImpl(DoctorRepository doctorRepository, PasswordEncoder passwordEncoder, SpecializationRepository specializationRepository, ModelMapper modelMapper) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.specializationRepository = specializationRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public void registerDoctor(DoctorRegisterRequest doctor) {
        Specialization specialization = specializationRepository
                .findByName(doctor.specialization())
                .orElseThrow(() -> new IllegalArgumentException("Specialization not found: " + doctor.specialization()));

        DoctorRegistrationDto doctorDto = new DoctorRegistrationDto(
                passwordEncoder.encode(doctor.password()),
                "ROLE_DOCTOR",
                doctor.email(),
                doctor.phone(),
                doctor.fullName(),
                specialization,
                String.valueOf(new Random().nextInt(8999999) + 1000000)
        );
        doctorRepository.save(modelMapper.map(doctorDto, Doctor.class));
    }


}

