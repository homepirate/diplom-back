package com.example.diplom.services.implementations;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.notif.NotificationService;
import com.example.diplom.repositories.*;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.DoctorRegistrationDto;
import com.example.diplom.services.dtos.VisitDto;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final VisitRepository visitRepository;
    private final ServiceRepository serviceRepository;
    private final SpecializationRepository specializationRepository;
    private final PatientRepository patientRepository;
    private final VisitServiceRepository visitServiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;
    private final AttachmentService attachmentService;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             VisitRepository visitRepository,
                             PasswordEncoder passwordEncoder,
                             ServiceRepository serviceRepository,
                             SpecializationRepository specializationRepository,
                             PatientRepository patientRepository,
                             ModelMapper modelMapper,
                             VisitServiceRepository visitServiceRepository,
                             NotificationService notificationService,
                             AttachmentService attachmentService) {
        this.doctorRepository = doctorRepository;
        this.visitRepository = visitRepository;
        this.serviceRepository = serviceRepository;
        this.specializationRepository = specializationRepository;
        this.patientRepository = patientRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.notificationService = notificationService;
        this.attachmentService = attachmentService;
    }

    // -------------------------------------------------
    // No special ownership check for registering a new doctor
    // -------------------------------------------------
    @Override
    public void registerDoctor(DoctorRegisterRequest doctor) {
        Specialization specialization = specializationRepository
                .findByName(doctor.specialization())
                .orElseThrow(() -> new IllegalArgumentException("Specialization not found: " + doctor.specialization()));

        DoctorRegistrationDto doctorDto = new DoctorRegistrationDto(
                doctor.password(), null, doctor.email(),
                doctor.phone(), doctor.fullName(), specialization, null
        );
        doctorDto.setPassword(passwordEncoder.encode(doctorDto.getPassword()));
        doctorDto.setRole("ROLE_DOCTOR");

        // Generate the unique code
        String uniqueCode = String.valueOf(new Random().nextInt(8999999) + 1000000);
        doctorDto.setUniqueCode(uniqueCode);

        doctorRepository.save(modelMapper.map(doctorDto, Doctor.class));
    }

    // -------------------------------------------------
    // GET VISITS BY MONTH, BY DAY
    // -------------------------------------------------

    /**
     * The only requirement is that the #doctorId matches the JWT's doctorId.
     */
    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public List<VisitDto> getDoctorVisitDates(UUID doctorId, int month, int year) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        return visitRepository.findByDoctorIdAndMonthYear(doctorId, month, year).stream()
                .map(visit -> {
                    VisitDto dto = modelMapper.map(visit, VisitDto.class);
                    dto.setFinished(visit.isFinished());
                    dto.setTotalCost(visit.getTotalCost());
                    dto.setNotes(visit.getNotes());
                    return dto;
                })
                .toList();
    }

    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public List<VisitDto> getDoctorVisitDatesByDay(UUID doctorId, String date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        return visitRepository.findByDoctorIdAndDate(doctorId, date).stream()
                .map(visit -> {
                    VisitDto dto = modelMapper.map(visit, VisitDto.class);
                    dto.setFinished(visit.isFinished());
                    dto.setTotalCost(visit.getTotalCost());
                    dto.setNotes(visit.getNotes());
                    return dto;
                })
                .toList();
    }

    // -------------------------------------------------
    // CREATE SERVICE
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        // check if name exists
        Optional<com.example.diplom.models.Service> existing = serviceRepository.findByDoctorIdAndName(doctorId, serviceRequest.name());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Service with that name already exists.");
        }

        com.example.diplom.models.Service newService = new com.example.diplom.models.Service();
        newService.setName(serviceRequest.name());
        newService.setPrice(serviceRequest.price());
        newService.setDoctor(doctor);

        serviceRepository.save(newService);
    }

    // -------------------------------------------------
    // GET DOCTOR SERVICES
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public List<ServiceResponse> getDoctorServices(UUID doctorId) {
        // Just ensures the doc from JWT == #doctorId
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        return serviceRepository.findByDoctorId(doctorId)
                .stream()
                .map(s -> new ServiceResponse(s.getName(), s.getPrice()))
                .toList();
    }

    // -------------------------------------------------
    // UPDATE SERVICE PRICE
    // -------------------------------------------------
    @Override
    @PreAuthorize(
            "@doctorAuthz.hasDoctorServiceOwnership(authentication, #doctorId, #updateServiceRequest.name())"
    )
    public void updateServicePrice(UUID doctorId, UpdateServiceRequest updateServiceRequest) {
        if (updateServiceRequest.price() == null || updateServiceRequest.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        com.example.diplom.models.Service service = serviceRepository.findByDoctorIdAndName(doctorId, updateServiceRequest.name())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with name '"
                        + updateServiceRequest.name() + "' for this doctor."));

        service.setPrice(updateServiceRequest.price());
        serviceRepository.save(service);
    }

    // -------------------------------------------------
    // GET DOCTOR PATIENTS
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public List<PatientResponse> getDoctorPatients(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        return doctor.getDoctorPatients().stream()
                .map(dp -> new PatientResponse(
                        dp.getPatient().getFullName(),
                        dp.getPatient().getBirthDate(),
                        dp.getPatient().getId()
                ))
                .toList();
    }

    // -------------------------------------------------
    // CREATE VISIT
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorPatientOwnership(authentication, #doctorId, #visitRequest.patientId())")
    public CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest) {
        // We already validated doc–patient link via @PreAuthorize
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));
        Patient patient = patientRepository.findById(visitRequest.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found " + visitRequest.patientId()));

        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setVisitDate(visitRequest.visitDate());
        visit.setNotes(visitRequest.notes());
        visit.setFinished(false);
        visit.setTotalCost(BigDecimal.ZERO);

        Visit saved = visitRepository.save(visit);
        notificationService.sendVisitCreatedNotification(patient.getEmail(), saved.getVisitDate().toString());

        return new CreateVisitResponse(saved.getVisitDate(), saved.getId());
    }

    // -------------------------------------------------
    // REARRANGE VISIT
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #rearrangeRequest.visitId())")
    public void rearrangeVisit(UUID doctorId, RearrangeVisitRequest rearrangeRequest) {
        // If we get in here, we know doc owns the visit
        Visit visit = visitRepository.findById(rearrangeRequest.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found " + rearrangeRequest.visitId()));

        visit.setVisitDate(rearrangeRequest.newVisitDate());
        visitRepository.save(visit);

        // notify patient
        notificationService.sendVisitCreatedNotification(visit.getPatient().getEmail(), visit.getVisitDate().toString());
    }

    // -------------------------------------------------
    // CANCEL VISIT
    // -------------------------------------------------
    /**
     * Notice that your cancelVisit method doesn't currently accept the doctorId
     * as a parameter. So either add it or you can do a custom PreAuthorize check
     * that extracts the docId from the JWT inside doctorAuthz.
     *
     * We'll add the parameter for consistency:
     */
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #visitIdRequest.id())")
    public void cancelVisit(UUID doctorId, VisitIdRequest visitIdRequest) {
        if (!visitRepository.existsById(visitIdRequest.id())) {
            throw new ResourceNotFoundException("Visit not found with id " + visitIdRequest.id());
        }
        visitRepository.deleteById(visitIdRequest.id());
    }

    // -------------------------------------------------
    // FINISH VISIT
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #finishVisitRequest.id())")
    public void finishVisit(UUID doctorId,FinishVisitRequest finishVisitRequest) {
        // We must also change signature to pass doctorId:
        // public void finishVisit(UUID doctorId, FinishVisitRequest finishVisitRequest) {...}
        //
        // For brevity, let's assume your method is now:
        //    finishVisit(UUID doctorId, FinishVisitRequest finishVisitRequest)

        Visit visit = visitRepository.findById(finishVisitRequest.id())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        if (finishVisitRequest.services() == null || finishVisitRequest.services().isEmpty()) {
            throw new IllegalArgumentException("At least one service must be provided.");
        }

        visit.setFinished(true);
        updateVisitServices(visit, finishVisitRequest.services());

        if (!finishVisitRequest.notes().equals(visit.getNotes())) {
            visit.setNotes(finishVisitRequest.notes());
        }

        BigDecimal totalCost = visitServiceRepository.findByVisit(visit).stream()
                .map(vs -> vs.getService().getPrice().multiply(BigDecimal.valueOf(vs.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        visit.setTotalCost(totalCost);

        visitRepository.save(visit);
    }

    // -------------------------------------------------
    // GET FINISH VISIT DATA
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #visitIdRequest.id())")
    public VisitDetailsResponse getFinishVisitData(UUID doctorId, VisitIdRequest visitIdRequest) {
        // Change signature to: getFinishVisitData(UUID doctorId, VisitIdRequest visitIdRequest)
        // for consistency with the same pattern
        Visit visit = visitRepository.findById(visitIdRequest.id())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found " + visitIdRequest.id()));

        List<com.example.diplom.models.Service> docServices = serviceRepository.findByDoctorId(doctorId);
        List<VisitService> visitServices = visitServiceRepository.findByVisit(visit);

        Map<UUID, Integer> serviceQuantities = visitServices.stream()
                .collect(Collectors.toMap(vs -> vs.getService().getId(), VisitService::getQuantity));

        List<VisitServicesDetailsResponse> services = docServices.stream()
                .map(s -> new VisitServicesDetailsResponse(
                        s.getId(),
                        s.getName(),
                        s.getPrice(),
                        serviceQuantities.getOrDefault(s.getId(), 0)
                ))
                .toList();

        // attachments
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

        return new VisitDetailsResponse(
                visit.getId(),
                visit.getVisitDate(),
                visit.isFinished(),
                visit.getNotes() != null ? visit.getNotes() : "",
                visit.getTotalCost(),
                services,
                attachmentUrls
        );
    }

    // -------------------------------------------------
    // GET PATIENT MEDICAL CARD
    // -------------------------------------------------
    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorPatientOwnership(authentication, #doctorId, #patientId)")
    public PatientMedCardResponse getPatientMedicalCard(UUID doctorId, UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found " + patientId));

        // All visits for this doc + that patient
        List<Visit> visits = visitRepository.findByPatientIdAndDoctorId(patientId, doctorId);
        // map them
        List<VisitDetailsResponse> visitDetails = visits.stream()
                .map(visit -> {
                    List<VisitService> vsList = visitServiceRepository.findByVisit(visit);

                    List<VisitServicesDetailsResponse> serviceResponses = vsList.stream()
                            .map(vs -> new VisitServicesDetailsResponse(
                                    vs.getServiceId(),
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

                    return new VisitDetailsResponse(
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

        return new PatientMedCardResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getBirthDate(),
                patient.getEmail(),
                patient.getPhone(),
                visitDetails
        );
    }

    // -------------------------------------------------
    // HELPER to handle updates of visit-services
    // -------------------------------------------------
    private void updateVisitServices(Visit visit, List<ServiceUpdateRequest> serviceUpdates) {
        List<VisitService> existing = visitServiceRepository.findByVisit(visit);
        Map<String, VisitService> existingMap = existing.stream()
                .collect(Collectors.toMap(vs -> vs.getService().getName(), Function.identity()));

        for (ServiceUpdateRequest update : serviceUpdates) {
            if (update.quantity() < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative for service: " + update.name());
            }
            com.example.diplom.models.Service service = serviceRepository
                    .findByDoctorIdAndName(visit.getDoctor().getId(), update.name())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + update.name()));

            if (existingMap.containsKey(update.name())) {
                VisitService vs = existingMap.get(update.name());
                vs.setQuantity(update.quantity());
                visitServiceRepository.save(vs);
            } else if (update.quantity() > 0) {
                VisitService newVs = new VisitService();
                newVs.setVisit(visit);
                newVs.setService(service);
                newVs.setQuantity(update.quantity());
                visitServiceRepository.save(newVs);
            }
        }
    }

}
