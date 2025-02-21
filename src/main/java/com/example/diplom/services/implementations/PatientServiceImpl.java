package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.PatientRegisterRequest;
import com.example.diplom.controllers.RR.PatientVisitDetailsResponse;
import com.example.diplom.controllers.RR.VisitServicesDetailsResponse;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.Patient;
import com.example.diplom.models.Visit;
import com.example.diplom.models.VisitService;
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

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final VisitRepository visitRepository;
    private final VisitServiceRepository visitServiceRepository;
    private final AttachmentService attachmentService;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, VisitRepository visitRepository,
                              VisitServiceRepository visitServiceRepository,
                              AttachmentService attachmentService) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.visitRepository = visitRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.attachmentService = attachmentService;
    }

    @Override
    public void registerPatient(PatientRegisterRequest patient) {
        PatientRegistrationDto patientDto = new PatientRegistrationDto(patient.password(),null, patient.email(), patient.phone(),
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
}

