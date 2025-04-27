package com.example.diplom.services.implementations;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.AlreadyLinkedException;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.models.PK.DoctorPatientPK;
import com.example.diplom.repositories.*;
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
    private final DoctorRepository doctorRepository;

    @Autowired
    public PatientServiceImpl(
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            ModelMapper modelMapper,
            VisitRepository visitRepository,
            VisitServiceRepository visitServiceRepository,
            AttachmentService attachmentService,
            DoctorPatientRepository doctorPatientRepository,
            ChatServiceImpl chatService,
            DoctorRepository doctorRepository
    ) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.visitRepository = visitRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.attachmentService = attachmentService;
        this.doctorPatientRepository = doctorPatientRepository;
        this.chatService = chatService;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public void registerPatient(PatientRegisterRequest request) {
        Optional<Patient> existingPatientOpt = patientRepository.findByPhone(request.phone());
        if (existingPatientOpt.isPresent()) {
            Patient existingPatient = existingPatientOpt.get();
            if (Boolean.TRUE.equals(existingPatient.getIsTemporary())) {
                existingPatient.setEmail(request.email());
                existingPatient.setFullName(request.fullName());
                existingPatient.setBirthDate(request.birthDate());
                existingPatient.setPassword(passwordEncoder.encode(request.password()));
                existingPatient.setRole("ROLE_PATIENT");
                existingPatient.setIsTemporary(false);
                patientRepository.save(existingPatient);
            } else {
                throw new IllegalArgumentException("Телефон уже зарегистрирован в системе");
            }
        } else {
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

        return visits.stream()
                .map(visit -> {
                    List<VisitService> visitServices = visitServiceRepository.findByVisit(visit);

                    List<VisitServicesDetailsResponse> serviceResponses = visitServices.stream()
                            .map(vs -> new VisitServicesDetailsResponse(
                                    vs.getService().getId(),
                                    vs.getService().getName(),
                                    vs.getService().getPrice(),
                                    vs.getQuantity()
                            ))
                            .toList();

                    List<String> attachmentUrls = buildAttachmentUrls(visit.getAttachments());

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
                })
                .toList();
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
                    List<Attachment> allAttachments = patient.getVisits().stream()
                            .flatMap(visit -> visit.getAttachments().stream())
                            .collect(Collectors.toList());
                    List<String> attachmentUrls = buildAttachmentUrls(allAttachments);
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
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        for (Visit visit : patient.getVisits()) {
            Set<Attachment> attachmentsCopy = Set.copyOf(visit.getAttachments());
            for (Attachment attachment : attachmentsCopy) {
                try {
                    attachmentService.deleteAttachmentById(attachment.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            visit.getAttachments().clear();
            visitRepository.save(visit);
        }

        String uniquePart = patient.getId().toString().substring(9, 12);
        patient.setPhone("удален" + LocalDate.now().toString() + uniquePart);
        patient.setFullName("удален" + LocalDate.now().toString() + uniquePart);
        patient.setEmail("удален" + LocalDate.now().toString() + uniquePart);
        patient.setBirthDate(LocalDate.now());
        patient.setPassword(passwordEncoder.encode("deleted"));
        patientRepository.save(patient);

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

    @Override
    public void linkPatientWithDoctor(UUID patientId, String doctorCode) {
        Doctor doctor = doctorRepository.findByUniqueCode(doctorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with provided code"));

        DoctorPatientPK pk = new DoctorPatientPK(doctor.getId(), patientId);
        if (doctorPatientRepository.existsById(pk)) {
            throw new AlreadyLinkedException("Patient is already linked with this doctor");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        DoctorPatient doctorPatient = new DoctorPatient(doctor, patient);
        doctorPatientRepository.save(doctorPatient);
    }

    private List<String> buildAttachmentUrls(Collection<Attachment> attachments) {
        return attachments.stream()
                .map(attachment -> {
                    try {
                        return attachmentService.getPresignedUrlForAttachment(attachment.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
