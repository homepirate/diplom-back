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
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

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

        List<VisitDto> visitDtos = visitRepository.findByDoctorId(doctor.getId()).stream()
                .map(visit -> modelMapper.map(visit, VisitDto.class))
                .toList();
        return visitDtos;
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

        // Сохранить визит
        Visit savedVisit = visitRepository.save(visit);


        notificationService.sendVisitCreatedNotification(
                patient.getEmail(),
                savedVisit.getVisitDate().toString()
        );

        // Если услуги были переданы, обработать добавление связей через отдельный метод
        if (visitRequest.services() != null && !visitRequest.services().isEmpty()) {
            createVisitServices(savedVisit, visitRequest.services());
        }

        return new CreateVisitResponse(
//                savedVisit.getId(),
//                patient.getId(),
//                doctor.getId(),
                savedVisit.getVisitDate()
        );
    }

    private void createVisitServices(Visit visit, List<String> serviceNames) {
        // Найти все указанные услуги
        List<com.example.diplom.models.Service> services = serviceRepository.findByNameIn(serviceNames);

        if (services.size() != serviceNames.size()) {
            throw new ResourceNotFoundException("One or more services not found: " + serviceNames);
        }

        // Создать связи между визитом и найденными услугами
        for (com.example.diplom.models.Service service : services) {
            VisitService visitService = new VisitService();
            visitService.setVisit(visit);
            visitService.setService(service);
            visitServiceRepository.save(visitService); // Сохранить связь
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
                        doctorPatient.getPatient().getBirthDate()
                ))
                .toList();
    }



}

