package com.example.diplom.utils;

import com.example.diplom.models.*;
import com.example.diplom.repositories.*;
import com.github.javafaker.Faker;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;


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
    private final MinioClient minioClient;

    private final Faker faker;
    private final PasswordEncoder passwordEncoder;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Autowired
    public DataInitializer(DoctorRepository doctorRepository, PatientRepository patientRepository,
                           VisitRepository visitRepository, ServiceRepository serviceRepository,
                           AttachmentRepository attachmentRepository, DoctorPatientRepository doctorPatientRepository,
                           VisitServiceRepository visitServiceRepository,
                           SpecializationRepository specializationRepository,
                           PasswordEncoder passwordEncoder,
                           MinioClient minioClient) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.serviceRepository = serviceRepository;
        this.attachmentRepository = attachmentRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.visitServiceRepository = visitServiceRepository;
        this.specializationRepository = specializationRepository;
        this.passwordEncoder = passwordEncoder;
        this.minioClient = minioClient;
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
                    boolean isFinished = faker.bool().bool();
                    visit.setFinished(isFinished);

                    // Set default totalCost to avoid NULL insertion
                    visit.setTotalCost(BigDecimal.ZERO);
                    visit = visitRepository.save(visit);

                    if (isFinished) {
                        // Fetch services for this doctor from the repository to avoid lazy initialization issues
                        List<Service> doctorServices = serviceRepository.findByDoctorId(doctor.getId());
                        if (!doctorServices.isEmpty()) {
                            Map<Service, VisitService> visitServiceMap = new HashMap<>();
                            BigDecimal totalCost = BigDecimal.ZERO;
                            int numServices = faker.number().numberBetween(1, 4);
                            for (int j = 0; j < numServices; j++) {
                                Service service = doctorServices.get(faker.number().numberBetween(0, doctorServices.size()));
                                Visit finalVisit = visit;
                                visitServiceMap.compute(service, (s, vs) -> {
                                    if (vs == null) {
                                        vs = new VisitService();
                                        vs.setVisit(finalVisit);
                                        vs.setService(service);
                                        vs.setQuantity(1);
                                    } else {
                                        vs.setQuantity(vs.getQuantity() + 1);
                                    }
                                    return vs;
                                });
                                totalCost = totalCost.add(service.getPrice());
                            }
                            visit.setTotalCost(totalCost);
                            visitRepository.save(visit);
                            // Save each VisitService entry
                            for (VisitService vs : visitServiceMap.values()) {
                                visitServiceRepository.save(vs);
                            }
                        }
                    } else {
                        // For unfinished visits, totalCost remains zero
                        visitRepository.save(visit);
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }
    }

    private void populateAttachments() {
        Set<Visit> visits = new HashSet<>(visitRepository.findAll());
        int counter = 0; // Counter to alternate file types

        for (Visit visit : visits) {
            int attachmentCount = faker.number().numberBetween(0, 3); // Each visit can have 0, 1, or 2 attachments

            for (int i = 0; i < attachmentCount; i++) {
                try {
                    ByteArrayInputStream bais;
                    byte[] fileContent;
                    String randomFileName;
                    String contentType;

                    if (counter % 2 == 0) {
                        // Generate a PDF file
                        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
                        try (PDDocument document = new PDDocument()) {
                            PDPage page = new PDPage();
                            document.addPage(page);

                            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                                contentStream.beginText();
                                contentStream.newLineAtOffset(100, 700);
                                contentStream.showText("Medical Report for Visit: " + visit.getId());
                                contentStream.newLineAtOffset(0, -20);
                                contentStream.showText("Doctor: " + visit.getDoctor().getFullName());
                                contentStream.newLineAtOffset(0, -20);
                                contentStream.showText("Patient: " + visit.getPatient().getFullName());
                                contentStream.newLineAtOffset(0, -20);
                                contentStream.showText("Notes: " + visit.getNotes());
                                contentStream.endText();
                            }

                            document.save(pdfOutputStream);
                        }

                        fileContent = pdfOutputStream.toByteArray();
                        randomFileName = UUID.randomUUID().toString() + ".pdf";
                        contentType = "application/pdf";
                    } else {
                        // Generate a TXT file
                        String randomContent = faker.lorem().paragraph();
                        fileContent = randomContent.getBytes(StandardCharsets.UTF_8);
                        randomFileName = UUID.randomUUID().toString() + ".txt";
                        contentType = "text/plain";
                    }

                    bais = new ByteArrayInputStream(fileContent);

                    // Upload the file to MinIO
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(randomFileName)
                                    .stream(bais, fileContent.length, -1)
                                    .contentType(contentType)
                                    .build()
                    );

                    // Save the attachment reference in the database
                    Attachment attachment = new Attachment();
                    attachment.setVisit(visit);
                    attachment.setFilePath(randomFileName);
                    attachment.setDescription(faker.lorem().sentence());
                    attachmentRepository.save(attachment);

                    counter++; // Alternate between PDF and TXT

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

    // Extra method to add additional VisitService entries only for finished visits
    private void populateExtraVisitServices() {
        Set<Visit> visits = new HashSet<>(visitRepository.findAll());
        for (Visit visit : visits) {
            if (!visit.isFinished()) {
                continue; // Skip unfinished visits
            }
            // Fetch doctor services via the repository to avoid lazy initialization
            List<Service> doctorServices = serviceRepository.findByDoctorId(visit.getDoctor().getId());
            if (doctorServices.isEmpty()) continue;
            if (faker.bool().bool()) { // Randomly decide whether to add an extra service
                Service service = doctorServices.get(faker.number().numberBetween(0, doctorServices.size()));
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
