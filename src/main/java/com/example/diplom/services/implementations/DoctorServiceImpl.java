package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.DoctorRegisterRequest;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.Doctor;
import com.example.diplom.repositories.DoctorRepository;
import com.example.diplom.repositories.VisitRepository;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.DoctorRegistrationDto;
import com.example.diplom.services.dtos.VisitDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final VisitRepository visitRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Autowired
    public DoctorServiceImpl(DoctorRepository doctorRepository, VisitRepository visitRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.doctorRepository = doctorRepository;
        this.visitRepository = visitRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Override
    public void registerDoctor(DoctorRegisterRequest doctor) {

        DoctorRegistrationDto doctorDto = new DoctorRegistrationDto(doctor.password(), null, doctor.email(),
                doctor.phone(), doctor.fullName(), doctor.specialization(), null);

        doctorDto.setPassword(passwordEncoder.encode(doctorDto.getPassword()));
        doctorDto.setRole("ROLE_DOCTOR");
        String uniqueCode = String.valueOf(new Random().nextInt(8999999) + 1000000);
        doctorDto.setUniqueCode(uniqueCode);

        doctorRepository.save(modelMapper.map(doctorDto, Doctor.class));
    }

    @Override
    public List<VisitDto> getDoctorVisitDates(UUID doctorId){
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));

        List<VisitDto> visitDtos = visitRepository.findByDoctorId(doctor.getId()).stream()
                .map(visit -> modelMapper.map(visit, VisitDto.class))
                .toList();
        return visitDtos;
    }
}

