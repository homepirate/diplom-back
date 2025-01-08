package com.example.diplom.services.implementations;


import com.example.diplom.models.Doctor;
import com.example.diplom.repositories.DoctorRepository;
import com.example.diplom.services.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorServiceImpl(DoctorRepository doctorRepository, PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registerDoctor(Doctor doctor) {
        doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));

        doctor.setRole("ROLE_DOCTOR");
        String uniqueCode = String.valueOf(new Random().nextInt(8999999) + 1000000);
        doctor.setUniqueCode(uniqueCode);

        doctorRepository.save(doctor);
    }
}

