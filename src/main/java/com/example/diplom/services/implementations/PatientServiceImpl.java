package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Patient;
import com.example.diplom.models.Visit;
import com.example.diplom.models.VisitService;
import com.example.diplom.repositories.DoctorPatientRepository;
import com.example.diplom.repositories.PatientRepository;
import com.example.diplom.repositories.VisitRepository;
import com.example.diplom.repositories.VisitServiceRepository;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.PatientService;
import com.example.diplom.services.dtos.PatientRegistrationDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final VisitRepository visitRepository;
    private final VisitServiceRepository visitServiceRepository;
    private final AttachmentService attachmentService;
    private final DoctorPatientRepository doctorPatientRepository;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, VisitRepository visitRepository, VisitServiceRepository visitServiceRepository, AttachmentService attachmentService, DoctorPatientRepository doctorPatientRepository) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.visitRepository = visitRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.attachmentService = attachmentService;
        this.doctorPatientRepository = doctorPatientRepository;
    }


    @Override
    public void registerPatient(PatientRegisterRequest patient) {
        PatientRegistrationDto patientDto = new PatientRegistrationDto(patient.password(), null, patient.email(), patient.phone(),
                patient.fullName(), patient.birthDate());
        patientDto.setPassword(passwordEncoder.encode(patientDto.getPassword()));
        patientDto.setRole("ROLE_PATIENT");

        patientRepository.save(modelMapper.map(patientDto, Patient.class));
    }

    @Override
    public List<PatientVisitDetailsResponse> getVisitsByPatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found " + patientId));

        List<Visit> visits = visitRepository.findByPatientId(patientId);

        return visits.stream().map(visit -> {
            List<VisitService> visitServices = visitServiceRepository.findByVisit(visit);

            List<VisitServicesDetailsResponse> serviceResponses = visitServices.stream()
                    .map(vs -> new VisitServicesDetailsResponse(
                            vs.getService().getId(),
                            vs.getService().getName(),
                            vs.getService().getPrice(),
                            vs.getQuantity()
                    ))
                    .toList();

            List<String> attachmentUrls = visit.getAttachments().stream()
                    .map(a -> {
                        try {
                            return attachmentService.getPresignedUrlForAttachment(a.getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return new PatientVisitDetailsResponse(
                    visit.getDoctor().getFullName(),
                    visit.getId(),
                    visit.getVisitDate(),
                    visit.isFinished(),
                    visit.getNotes() != null ? visit.getNotes() : "",
                    visit.getTotalCost(),
                    serviceResponses,
                    attachmentUrls
            );
        }).toList();
    }

    @Override
    public List<DoctorResponse> getPatientDoctors(UUID patientId) {
        List<Doctor> doctors = doctorPatientRepository.findDoctorsByPatientId(patientId);

        return doctors.stream()
                .map(doctor -> new DoctorResponse(
                        doctor.getFullName(),
                        doctor.getSpecializationName(),
                        doctor.getId()
                ))
                .toList();
    }

    @Override
    public PatientProfileResponse profileById(UUID patientId) throws ResourceNotFoundException {
        return patientRepository.findById(patientId)
                .map(patient -> {
                    List<String> attachmentUrls = patient.getVisits().stream()
                            .flatMap(visit -> visit.getAttachments().stream())
                            .map(attachment -> {
                                try {
                                    return attachmentService.getPresignedUrlForAttachment(attachment.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return new PatientProfileResponse(
                            patient.getFullName(),
                            patient.getBirthDate(),
                            patient.getEmail(),
                            patient.getPhone(),
                            attachmentUrls
                    );
                })
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }





    @Override
    public void updatePatientProfile(UUID patientId, UpdatePatientProfileRequest updateRequest) throws ResourceNotFoundException {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        if (updateRequest.fullName() != null) {
            patient.setFullName(updateRequest.fullName());
        }
        if (updateRequest.birthDate() != null) {
            patient.setBirthDate(updateRequest.birthDate());
        }
        if (updateRequest.email() != null) {
            patient.setEmail(updateRequest.email());
        }
        if (updateRequest.phone() != null) {
            patient.setPhone(updateRequest.phone());
        }
        patientRepository.save(patient);
    }


}

