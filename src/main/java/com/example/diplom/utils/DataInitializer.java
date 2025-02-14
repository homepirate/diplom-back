package com.example.diplom.utils;

import com.example.diplom.models.*;
import com.example.diplom.repositories.*;
import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final ServiceRepository serviceRepository;
    private final AttachmentRepository attachmentRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final VisitServiceRepository visitServiceRepository;
    private final SpecializationRepository specializationRepository;

    private final Faker faker;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(DoctorRepository doctorRepository, PatientRepository patientRepository,
                           VisitRepository visitRepository, ServiceRepository serviceRepository,
                           AttachmentRepository attachmentRepository, DoctorPatientRepository doctorPatientRepository,
                           VisitServiceRepository visitServiceRepository,
                           SpecializationRepository specializationRepository,
                           PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.serviceRepository = serviceRepository;
        this.attachmentRepository = attachmentRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.specializationRepository = specializationRepository;
        this.passwordEncoder = passwordEncoder;
        this.faker = new Faker();
    }

    @PostConstruct
    public void init() {
        populateDoctors();
        populatePatients();
        populateServices();
        populateVisits();
        populateAttachments();
        populateDoctorPatientLinks();
        populateExtraVisitServices();
    }

    private void populateSpecializations() {
        String[] specializations = {"Cardiology", "Neurology", "Orthopedics", "Dermatology", "Pediatrics"};
        for (String name : specializations) {
            Specialization specialization = new Specialization(name);
            specializationRepository.save(specialization);
        }
    }

    private void populateDoctors() {
        populateSpecializations();
        for (int i = 0; i < 10; i++) {
            Doctor doctor = new Doctor();
            doctor.setFullName(faker.name().fullName());
            doctor.setEmail(faker.internet().emailAddress());
            doctor.setPhone("8" + faker.number().digits(10));
            doctor.setPassword(passwordEncoder.encode("password"));
            doctor.setRole("ROLE_DOCTOR");
            doctor.setUniqueCode(faker.number().digits(7));

            Specialization specialization = specializationRepository.findAll()
                    .stream()
                    .skip(faker.number().numberBetween(0, (int) specializationRepository.count()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No specializations found"));

            doctor.setSpecialization(specialization);
            doctorRepository.save(doctor);
        }
    }

    private void populatePatients() {
        for (int i = 0; i < 100; i++) {
            Patient patient = new Patient();
            patient.setFullName(faker.name().fullName());
            patient.setBirthDate(faker.date().birthday(18, 80)
                    .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            patient.setEmail(faker.internet().emailAddress());
            patient.setPhone("8" + faker.number().digits(10));
            patient.setPassword(passwordEncoder.encode("password"));
            patient.setRole("ROLE_PATIENT");
            patientRepository.save(patient);
        }
    }

    private void populateServices() {
        Set<Doctor> doctors = new HashSet<>(doctorRepository.findAll());

        for (Doctor doctor : doctors) {
            Set<String> existingServiceNames = new HashSet<>();

            for (int i = 0; i < 3; i++) {
                String serviceName;
                do {
                    serviceName = faker.company().buzzword();
                } while (existingServiceNames.contains(serviceName) ||
                        serviceRepository.findByDoctorIdAndName(doctor.getId(), serviceName).isPresent());
                existingServiceNames.add(serviceName);

                Service service = new Service();
                service.setName(serviceName);
                service.setPrice(BigDecimal.valueOf(faker.number().randomDouble(2, 50, 500)));
                service.setDoctor(doctor);

                serviceRepository.save(service);
            }
        }
    }

    private void populateVisits() {
        Set<Patient> patients = new HashSet<>(patientRepository.findAll());
        Set<Doctor> doctors = new HashSet<>(doctorRepository.findAll());
        // Get the list of all services
        List<Service> services = serviceRepository.findAll();

        LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = LocalDateTime.now();

        for (Doctor doctor : doctors) {
            LocalDateTime currentDate = startDate;
            while (currentDate.isBefore(endDate)) {
                for (int i = 0; i < 8; i++) {
                    Visit visit = new Visit();

                    Patient patient = patients.stream()
                            .skip(faker.number().numberBetween(0, patients.size()))
                            .findFirst().orElse(null);
                    if (patient == null) continue;

                    visit.setDoctor(doctor);
                    visit.setPatient(patient);
                    visit.setVisitDate(currentDate.plusHours(faker.number().numberBetween(0, 12)));
                    visit.setNotes(faker.lorem().sentence());
                    visit.setFinished(faker.bool().bool());

                    // Group VisitServices by Service to set quantities
                    Map<Service, VisitService> visitServiceMap = new HashMap<>();
                    BigDecimal totalCost = BigDecimal.ZERO;

                    int numServices = faker.number().numberBetween(1, 4);
                    for (int j = 0; j < numServices; j++) {
                        Service service = services.stream()
                                .skip(faker.number().numberBetween(0, services.size()))
                                .findFirst().orElse(null);
                        if (service != null) {
                            // Update quantity if the service already exists for this visit
                            if (visitServiceMap.containsKey(service)) {
                                VisitService existing = visitServiceMap.get(service);
                                existing.setQuantity(existing.getQuantity() + 1);
                            } else {
                                VisitService vs = new VisitService();
                                vs.setVisit(visit);
                                vs.setService(service);
                                vs.setQuantity(1);
                                visitServiceMap.put(service, vs);
                            }
                            totalCost = totalCost.add(service.getPrice());
                        }
                    }

                    visit.setTotalCost(totalCost);
                    visit = visitRepository.save(visit);

                    // Save grouped VisitService entries
                    for (VisitService vs : visitServiceMap.values()) {
                        vs.setVisit(visit); // Ensure the visit is set
                        visitServiceRepository.save(vs);
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }
    }

    private void populateAttachments() {
        Set<Visit> visits = new HashSet<>(visitRepository.findAll());
        for (Visit visit : visits) {
            Attachment attachment = new Attachment();
            attachment.setVisit(visit);
            attachment.setFilePath(faker.file().fileName());
            attachment.setDescription(faker.lorem().sentence());
            attachmentRepository.save(attachment);
        }
    }

    private void populateDoctorPatientLinks() {
        Set<Doctor> doctors = new HashSet<>(doctorRepository.findAll());
        Set<Patient> patients = new HashSet<>(patientRepository.findAll());

        for (Doctor doctor : doctors) {
            for (Patient patient : patients) {
                DoctorPatient doctorPatient = new DoctorPatient(doctor, patient);
                doctorPatientRepository.save(doctorPatient);
            }
        }
    }

    // Extra method to add additional VisitService entries (if needed)
    private void populateExtraVisitServices() {
        Set<Visit> visits = new HashSet<>(visitRepository.findAll());
        List<Service> services = serviceRepository.findAll();
        for (Visit visit : visits) {
            if (faker.bool().bool()) { // randomly decide whether to add an extra service
                Service service = services.stream()
                        .skip(faker.number().numberBetween(0, services.size()))
                        .findFirst().orElse(null);
                if (service != null) {
                    // Check if a VisitService already exists for this service in the visit
                    Optional<VisitService> existingOpt = visitServiceRepository.findByVisit(visit).stream()
                            .filter(vs -> vs.getService().getId().equals(service.getId()))
                            .findFirst();
                    VisitService vs;
                    if (existingOpt.isPresent()) {
                        vs = existingOpt.get();
                        vs.setQuantity(vs.getQuantity() + 1);
                    } else {
                        vs = new VisitService();
                        vs.setVisit(visit);
                        vs.setService(service);
                        vs.setQuantity(1);
                    }
                    visitServiceRepository.save(vs);
                    visit.setTotalCost(visit.getTotalCost().add(service.getPrice()));
                    visitRepository.save(visit);
                }
            }
        }
    }
}
