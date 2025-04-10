package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.repositories.DoctorPatientRepository;
import com.example.diplom.repositories.PatientRepository;
import com.example.diplom.repositories.VisitRepository;
import com.example.diplom.repositories.VisitServiceRepository;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.PatientService;
import com.example.diplom.services.dtos.PatientRegistrationDto;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.substring;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final VisitRepository visitRepository;
    private final VisitServiceRepository visitServiceRepository;
    private final AttachmentService attachmentService;
    private final DoctorPatientRepository doctorPatientRepository;
    private final ChatServiceImpl chatService;

    @Autowired
    public PatientServiceImpl(PatientRepository patientRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, VisitRepository visitRepository, VisitServiceRepository visitServiceRepository, AttachmentService attachmentService, DoctorPatientRepository doctorPatientRepository, ChatServiceImpl chatService) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.visitRepository = visitRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.attachmentService = attachmentService;
        this.doctorPatientRepository = doctorPatientRepository;
        this.chatService = chatService;

    }


    @Override
    public void registerPatient(PatientRegisterRequest request) {
        // Look for an existing patient with this phone
        Optional<Patient> existingPatientOpt = patientRepository.findByPhone(request.phone());
        if (existingPatientOpt.isPresent()) {
            Patient existingPatient = existingPatientOpt.get();
            if (Boolean.TRUE.equals(existingPatient.getIsTemporary())) {
                // Migrate the temporary record
                existingPatient.setEmail(request.email());
                existingPatient.setFullName(request.fullName());
                existingPatient.setBirthDate(request.birthDate());
                existingPatient.setPassword(passwordEncoder.encode(request.password()));
                existingPatient.setRole("ROLE_PATIENT");
                existingPatient.setIsTemporary(false); // mark as fully registered
                patientRepository.save(existingPatient);
            } else {
                // Phone already exists for a fully registered patient
                throw new IllegalArgumentException("Телефон уже зарегистрирован в системе");
            }
        } else {
            // No record exists; create a new patient.
            PatientRegistrationDto dto = new PatientRegistrationDto(
                    request.password(),
                    null,
                    request.email(),
                    request.phone(),
                    request.fullName(),
                    request.birthDate()
            );
            dto.setPassword(passwordEncoder.encode(dto.getPassword()));
            dto.setRole("ROLE_PATIENT");
            Patient newPatient = modelMapper.map(dto, Patient.class);
            newPatient.setIsTemporary(false);
            patientRepository.save(newPatient);
        }
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

    @Transactional
    @Override
    public void deleteAllPatientData(UUID patientId) {
        // Retrieve the patient
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        // For each visit of the patient, delete all attachments using the new method.
        // Create a copy of the attachments collection to avoid ConcurrentModificationException.
        for (Visit visit : patient.getVisits()) {
            Set<Attachment> attachmentsCopy = Set.copyOf(visit.getAttachments());
            for (Attachment attachment : attachmentsCopy) {
                try {
                    // Call the new method to delete attachment by its id.
                    attachmentService.deleteAttachmentById(attachment.getId());
                } catch (Exception e) {
                    // Log or handle the exception as needed
                    e.printStackTrace();
                }
            }
            // Optional: Clear the attachment set from the visit once all have been processed.
            visit.getAttachments().clear();
            // Save the visit update if needed.
            visitRepository.save(visit);
        }

        // Depersonalize the patient's data by updating PII fields
        // Depersonalize the patient’s data by updating PII fields

        String uniquePart = patient.getId().toString().substring(9, 13); // characters 10 to 13
        patient.setPhone("удален " + LocalDate.now().toString() + uniquePart);
        patient.setFullName("удален " + LocalDate.now().toString() + uniquePart);
        patient.setEmail("удален " + LocalDate.now().toString() + uniquePart);
// Append the patient ID to ensure phone is unique.

        patient.setBirthDate(LocalDate.now());
        patient.setPassword(passwordEncoder.encode("deleted"));

// Save the patient record after depersonalization
        patientRepository.save(patient);

// Delete all chat messages for this user.
// (Assuming the patient's ID is used as the chat user identifier; otherwise adjust accordingly.)
        chatService.deleteAllMessagesForUser(patient.getId().toString());

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

