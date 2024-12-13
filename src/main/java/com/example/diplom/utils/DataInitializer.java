package com.example.diplom.utils;

import com.example.diplom.models.*;
import com.example.diplom.repositories.*;
import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final ServiceRepository serviceRepository;
    private final AttachmentRepository attachmentRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final VisitServiceRepository visitServiceRepository;
    private final Faker faker;

    @Autowired
    public DataInitializer(DoctorRepository doctorRepository, PatientRepository patientRepository,
                           VisitRepository visitRepository, ServiceRepository serviceRepository,
                           AttachmentRepository attachmentRepository, DoctorPatientRepository doctorPatientRepository, VisitServiceRepository visitServiceRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.serviceRepository = serviceRepository;
        this.attachmentRepository = attachmentRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.visitServiceRepository = visitServiceRepository;
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
        populateVisitServices();
    }

    private void populateDoctors() {
        for (int i = 0; i < 10; i++) {
            Doctor doctor = new Doctor();
            doctor.setFullName(faker.name().fullName());
            doctor.setSpecialization(faker.job().title());
            doctor.setEmail(faker.internet().emailAddress());


            String uniqueCode = faker.idNumber().valid();
            if (uniqueCode.length() > 7) {
                uniqueCode = uniqueCode.substring(0, 7);
            }
            doctor.setUniqueCode(uniqueCode);


            String phone = "8" + faker.number().digits(10);
            doctor.setPhone(phone);

            doctorRepository.save(doctor);
        }
    }


    private void populatePatients() {
        for (int i = 0; i < 100; i++) {
            Patient patient = new Patient();
            patient.setName(faker.name().fullName());
            patient.setBirthDate(faker.date().birthday(18, 80).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            String phone = "8" + faker.number().digits(10);
            patient.setPhone(phone);
            patient.setEmail(faker.internet().emailAddress());
            patientRepository.save(patient);
        }
    }

    private void populateVisits() {
        Set<Patient> patients = new HashSet<>(patientRepository.findAll());
        Set<Doctor> doctors = new HashSet<>(doctorRepository.findAll());
        for (int i = 0; i < 50; i++) {
            Visit visit = new Visit();
            visit.setPatient(faker.random().nextBoolean() ? patients.stream().findAny().get() : patients.stream().skip(faker.number().numberBetween(0, 100)).findFirst().get());
            visit.setDoctor(faker.random().nextBoolean() ? doctors.stream().findAny().get() : doctors.stream().skip(faker.number().numberBetween(0, 10)).findFirst().get());
            visit.setVisitDate(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 365)));
            visit.setNotes(faker.lorem().sentence());
            visitRepository.save(visit);
        }
    }

    private void populateServices() {
        Set<Doctor> doctors = new HashSet<>(doctorRepository.findAll());
        for (Doctor doctor : doctors) {
            for (int i = 0; i < 3; i++) {
                Service service = new Service();
                service.setName(faker.company().bs());
                service.setPrice(BigDecimal.valueOf(faker.number().randomDouble(2, 50, 500)));
                service.setDoctor(doctor);
                serviceRepository.save(service);
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

    private void populateVisitServices() {
        Set<Visit> visits = new HashSet<>(visitRepository.findAll());
        Set<Service> services = new HashSet<>(serviceRepository.findAll());
        for (Visit visit : visits) {

            for (int i = 0; i < 2; i++) {
                Service service = faker.random().nextBoolean() ? services.stream().findAny().get() : services.stream().skip(faker.number().numberBetween(0, services.size())).findFirst().get();
                VisitService visitService = new VisitService();
                visitService.setVisit(visit);
                visitService.setService(service);
                visitServiceRepository.save(visitService);
            }
        }
    }
}

