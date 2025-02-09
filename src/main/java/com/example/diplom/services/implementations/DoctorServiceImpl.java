package com.example.diplom.services.implementations;


import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.notif.NotificationService;
import com.example.diplom.repositories.*;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.DoctorRegistrationDto;
import com.example.diplom.services.dtos.VisitDto;
import org.modelmapper.ModelMapper;
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
    private final PasswordEncoder passwordEncoder;
    private final ServiceRepository serviceRepository;
    private final SpecializationRepository specializationRepository;
    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;
    private VisitServiceRepository visitServiceRepository;

    private final NotificationService notificationService;


    public DoctorServiceImpl(DoctorRepository doctorRepository, VisitRepository visitRepository, PasswordEncoder passwordEncoder, ServiceRepository serviceRepository, SpecializationRepository specializationRepository, PatientRepository patientRepository, ModelMapper modelMapper, VisitServiceRepository visitServiceRepository, NotificationService notificationService) {
        this.doctorRepository = doctorRepository;
        this.visitRepository = visitRepository;
        this.passwordEncoder = passwordEncoder;
        this.serviceRepository = serviceRepository;
        this.specializationRepository = specializationRepository;
        this.patientRepository = patientRepository;
        this.modelMapper = modelMapper;
        this.visitServiceRepository = visitServiceRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void registerDoctor(DoctorRegisterRequest doctor) {
        Specialization specialization = specializationRepository
                .findByName(doctor.specialization())
                .orElseThrow(() -> new IllegalArgumentException("Specialization not found: " + doctor.specialization()));

        DoctorRegistrationDto doctorDto = new DoctorRegistrationDto(doctor.password(), null, doctor.email(),
                doctor.phone(), doctor.fullName(), specialization, null);

        doctorDto.setPassword(passwordEncoder.encode(doctorDto.getPassword()));
        doctorDto.setRole("ROLE_DOCTOR");
        String uniqueCode = String.valueOf(new Random().nextInt(8999999) + 1000000);
        doctorDto.setUniqueCode(uniqueCode);

        doctorRepository.save(modelMapper.map(doctorDto, Doctor.class));
    }

    @Override
    public List<VisitDto> getDoctorVisitDates(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));

        return visitRepository.findByDoctorId(doctor.getId()).stream()
                .map(visit -> {
                    VisitDto visitDto = modelMapper.map(visit, VisitDto.class);
                    visitDto.setFinished(visit.isFinished());
                    visitDto.setTotalCost(visit.getTotalCost());
                    return visitDto;
                })
                .toList();
    }


    @Override
    public void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));

        // Check if the doctor already has a service with the same name
        Optional<com.example.diplom.models.Service> existingService = serviceRepository.findByDoctorIdAndName(doctorId, serviceRequest.name());
        if (existingService.isPresent()) {
            throw new IllegalArgumentException("A service with this name already exists for this doctor.");
        }

        com.example.diplom.models.Service service = new com.example.diplom.models.Service();
        service.setName(serviceRequest.name());
        service.setPrice(serviceRequest.price());
        service.setDoctor(doctor);

        serviceRepository.save(service);
    }


    @Override
    public CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest) {
        // Найти доктора
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));

        // Найти пациента
        Patient patient = patientRepository.findById(visitRequest.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + visitRequest.patientId()));

        // Создать новую сущность визита
        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setVisitDate(visitRequest.visitDate());
        visit.setNotes(visitRequest.notes()); // Заметки могут быть `null`
        visit.setFinished(false);
        visit.setTotalCost(BigDecimal.ZERO);
        // Сохранить визит
        Visit savedVisit = visitRepository.save(visit);


        notificationService.sendVisitCreatedNotification(
                patient.getEmail(),
                savedVisit.getVisitDate().toString()
        );


        return new CreateVisitResponse(
//                savedVisit.getId(),
//                patient.getId(),
//                doctor.getId(),
                savedVisit.getVisitDate()
        );
    }

    private void createVisitServices(Visit visit, List<String> serviceNames) {
        // Group service names and count occurrences
        Map<String, Long> serviceCount = serviceNames.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Get the distinct service names from the keys
        List<String> distinctNames = new ArrayList<>(serviceCount.keySet());

        // Fetch the corresponding Service entities using the distinct names
        List<com.example.diplom.models.Service> services = serviceRepository.findByNameIn(distinctNames);

        // Ensure that all required services were found
        if (services.size() != distinctNames.size()) {
            Set<String> foundNames = services.stream()
                    .map(com.example.diplom.models.Service::getName)
                    .collect(Collectors.toSet());
            List<String> missing = distinctNames.stream()
                    .filter(name -> !foundNames.contains(name))
                    .collect(Collectors.toList());
            throw new ResourceNotFoundException("Services not found: " + missing);
        }

        // Build a lookup map by service name
        Map<String, com.example.diplom.models.Service> serviceMap = services.stream()
                .collect(Collectors.toMap(com.example.diplom.models.Service::getName, Function.identity()));

        // For each distinct service, create one VisitService record with its quantity
        for (Map.Entry<String, Long> entry : serviceCount.entrySet()) {
            String serviceName = entry.getKey();
            int quantity = entry.getValue().intValue();
            VisitService vs = new VisitService();
            vs.setVisit(visit);
            vs.setService(serviceMap.get(serviceName));
            vs.setQuantity(quantity);
            visitServiceRepository.save(vs);
        }
    }



    @Override
    public List<ServiceResponse> getDoctorServices(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));

        return serviceRepository.findByDoctorId(doctorId).stream()
                .map(service -> new ServiceResponse(service.getName(), service.getPrice()))
                .toList();
    }

    @Override
    public void updateServicePrice(UUID doctorId, UpdateServiceRequest updateServiceRequest) {
        if (updateServiceRequest.price() == null || updateServiceRequest.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be a positive value.");
        }

        // Find the service by doctor ID and name
        com.example.diplom.models.Service service = serviceRepository.findByDoctorIdAndName(doctorId, updateServiceRequest.name())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with name '" + updateServiceRequest.name() + "' for this doctor."));

        // Update the price
        service.setPrice(updateServiceRequest.price());

        // Save the updated service
        serviceRepository.save(service);
    }

    @Override
    public List<PatientResponse> getDoctorPatients(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id " + doctorId));

        return doctor.getDoctorPatients().stream()
                .map(doctorPatient -> new PatientResponse(
                        doctorPatient.getPatient().getFullName(),
                        doctorPatient.getPatient().getBirthDate(),
                        doctorPatient.getPatientId()
                ))
                .toList();
    }

    @Override
    public void rearrangeVisit(UUID doctorId, RearrangeVisitRequest rearrangeRequest) {
        Visit visit = visitRepository.findById(rearrangeRequest.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found with id " + rearrangeRequest.visitId()));

        if (!visit.getDoctor().getId().equals(doctorId)) {
            throw new SecurityException("Doctor is not authorized to modify this visit.");
        }

        visit.setVisitDate(rearrangeRequest.newVisitDate());
        visitRepository.save(visit);

        // Notify patient about rearrangement
        notificationService.sendVisitCreatedNotification(
                visit.getPatient().getEmail(),
                visit.getVisitDate().toString()
        );
    }

    @Override
    public void cancelVisit(VisitIdRequest visitIdRequest) {
        if (!visitRepository.existsById(visitIdRequest.id())) {
            throw new ResourceNotFoundException("Visit not found with id " + visitIdRequest.id());
        }
        visitRepository.deleteById(visitIdRequest.id());
    }

    @Override
    public VisitNotesResponse getVisitDescription(VisitIdRequest visitIdRequest) {
        return new VisitNotesResponse(visitRepository.getNotesById(visitIdRequest.id()));
    }

    @Override
    public void finishVisit(FinishVisitRequest finishVisitRequest) {
        // Step 1: Retrieve the visit based on the visit ID
        Visit visit = visitRepository.findById(finishVisitRequest.id())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found with id " + finishVisitRequest.id()));

        // Step 2: Ensure that at least one service is provided
        if (finishVisitRequest.services() == null || finishVisitRequest.services().isEmpty()) {
            throw new IllegalArgumentException("At least one service must be provided to finish the visit.");
        }

        // Step 3: Mark the visit as finished
        visit.setFinished(true);

        // Step 4: Add the provided services to the visit
        createVisitServices(visit, finishVisitRequest.services());

        // Step 5: Update the notes only if they are different
        if ( !finishVisitRequest.notes().equals(visit.getNotes())) {
            visit.setNotes(finishVisitRequest.notes());
        }

        // Step 6: Recalculate the total cost based on the added services
        BigDecimal totalCost = visitServiceRepository.findByVisit(visit).stream()
                .map(vs -> vs.getService().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        visit.setTotalCost(totalCost);

        // Step 7: Save the updated visit
        visitRepository.save(visit);

        // Step 8: Notify the patient that the visit has been completed
       /* notificationService.sendVisitFinishedNotification(
                visit.getPatient().getEmail(),
                visit.getVisitDate().toString()
        );*/
    }


}

