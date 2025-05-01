package com.example.diplom.utils;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.models.*;
import com.example.diplom.repositories.*;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.ChatService;
import com.github.javafaker.Faker;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.mock.web.MockMultipartFile;

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
    private final AttachmentService attachmentService;
    private final ChatService chatService;        // 1. Зависимость на ChatService


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
                           MinioClient minioClient, AttachmentService attachmentService, ChatService chatService) {
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
        this.attachmentService = attachmentService;
        this.chatService = chatService;
        this.faker = new Faker();
    }

    @PostConstruct
    public void init() {
        populateDoctors();
        populatePatients();
        populateDoctorPatientLinks();
        populateServices();
        populateVisits();
        populateAttachments();
        populateExtraVisitServices();
        populateChatMessages();
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

    private void populateDoctorPatientLinks() {
        List<Doctor> doctors = doctorRepository.findAll();
        List<Patient> patients = patientRepository.findAll();
        Random random = new Random();
        Set<UUID> connectedPatientIds = new HashSet<>();

        // For each doctor, assign a random subset of patients (at least one)
        for (Doctor doctor : doctors) {
            Collections.shuffle(patients, random);
            int numberOfConnections = random.nextInt(patients.size()) + 1; // at least one connection
            for (int i = 0; i < numberOfConnections; i++) {
                Patient patient = patients.get(i);
                DoctorPatient doctorPatient = new DoctorPatient(doctor, patient);
                doctorPatientRepository.save(doctorPatient);
                connectedPatientIds.add(patient.getId());
            }
        }

        // Ensure every patient is connected to at least one doctor
        for (Patient patient : patients) {
            if (!connectedPatientIds.contains(patient.getId())) {
                Doctor randomDoctor = doctors.get(random.nextInt(doctors.size()));
                DoctorPatient doctorPatient = new DoctorPatient(randomDoctor, patient);
                doctorPatientRepository.save(doctorPatient);
            }
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
        // Build a map of linked patients for each doctor using the doctor's id as key.
        List<DoctorPatient> allLinks = doctorPatientRepository.findAll();
        Map<UUID, List<Patient>> doctorPatientsMap = new HashMap<>();
        for (DoctorPatient dp : allLinks) {
            UUID doctorId = dp.getDoctor().getId();
            doctorPatientsMap.computeIfAbsent(doctorId, k -> new ArrayList<>()).add(dp.getPatient());
        }
        List<Doctor> doctors = doctorRepository.findAll();

        LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = LocalDateTime.now();

        for (Doctor doctor : doctors) {
            List<Patient> linkedPatients = doctorPatientsMap.get(doctor.getId());
            if (linkedPatients == null || linkedPatients.isEmpty()) {
                continue;
            }
            LocalDateTime currentDate = startDate;
            while (currentDate.isBefore(endDate)) {
                for (int i = 0; i < 8; i++) {
                    Visit visit = new Visit();
                    Patient patient = linkedPatients.get(faker.number().numberBetween(0, linkedPatients.size()));
                    visit.setDoctor(doctor);
                    visit.setPatient(patient);
                    visit.setVisitDate(currentDate.plusHours(faker.number().numberBetween(0, 12)));
                    visit.setNotes(faker.lorem().sentence());
                    boolean isFinished = faker.bool().bool();
                    visit.setFinished(isFinished);
                    visit.setTotalCost(BigDecimal.ZERO);
                    visit = visitRepository.save(visit);

                    if (isFinished) {
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
                            for (VisitService vs : visitServiceMap.values()) {
                                visitServiceRepository.save(vs);
                            }
                        }
                    } else {
                        visitRepository.save(visit);
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }
    }
    public void populateAttachments() {
        List<Visit> visits = visitRepository.findAll();
        int counter = 0;

        for (Visit visit : visits) {
            int attachmentCount = faker.number().numberBetween(0, 3);

            for (int i = 0; i < attachmentCount; i++) {
                try {
                    byte[] fileContent;
                    String randomFileName;
                    String contentType;

                    // Генерируем PDF или TXT
                    if (counter % 2 == 0) {
                        // --- PDF ---
                        ByteArrayOutputStream pdfOs = new ByteArrayOutputStream();
                        try (PDDocument document = new PDDocument()) {
                            PDPage page = new PDPage();
                            document.addPage(page);
                            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                                cs.beginText();
                                cs.newLineAtOffset(100, 700);
                                cs.showText("Medical Report for Visit: " + visit.getId());
                                cs.newLineAtOffset(0, -20);
                                cs.showText("Doctor: " + visit.getDoctor().getFullName());
                                cs.newLineAtOffset(0, -20);
                                cs.showText("Patient: " + visit.getPatient().getFullName());
                                cs.newLineAtOffset(0, -20);
                                cs.showText("Notes: " + visit.getNotes());
                                cs.endText();
                            }
                            document.save(pdfOs);
                        }
                        fileContent = pdfOs.toByteArray();
                        randomFileName = UUID.randomUUID() + ".pdf";
                        contentType = "application/pdf";

                    } else {
                        // --- TXT ---
                        String txt = faker.lorem().paragraph();
                        fileContent = txt.getBytes(StandardCharsets.UTF_8);
                        randomFileName = UUID.randomUUID() + ".txt";
                        contentType = "text/plain";
                    }

                    // Оборачиваем в MultipartFile
                    MockMultipartFile multipartFile = new MockMultipartFile(
                            "file",                  // поле формы
                            randomFileName,          // исходное имя файла
                            contentType,             // MIME
                            fileContent              // данные
                    );

                    // Формируем запрос и вызываем сервис
                    AddAttachmentRequest req = new AddAttachmentRequest(
                            visit.getId(),
                            multipartFile,
                            faker.lorem().sentence()
                    );

                    attachmentService.addAttachment(
                            visit.getPatient().getId(),
                            req
                    );

                    counter++;

                } catch (Exception e) {
                    // Логируем, но не останавливаемся на ошибке одного файла
                    e.printStackTrace();
                }
            }
        }
    }

    private void populateExtraVisitServices() {
        Set<Visit> visits = new HashSet<>(visitRepository.findAll());
        for (Visit visit : visits) {
            if (!visit.isFinished()) {
                continue;
            }
            List<Service> doctorServices = serviceRepository.findByDoctorId(visit.getDoctor().getId());
            if (doctorServices.isEmpty()) continue;
            if (faker.bool().bool()) {
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


    private void populateChatMessages() {
        // Получаем все связи «врач–пациент»
        List<DoctorPatient> links = doctorPatientRepository.findAll();

        for (DoctorPatient dp : links) {
            String doctorId  = dp.getDoctor().getId().toString();
            String patientId = dp.getPatient().getId().toString();

            // Случайное количество сообщений в одну сторону
            int count = faker.number().numberBetween(1, 5);
            for (int i = 0; i < count; i++) {
                // Врач → Пациент
                ChatMessage msgFromDoctor = new ChatMessage();
                msgFromDoctor.setSenderId(doctorId);
                msgFromDoctor.setReceiverId(patientId);
                msgFromDoctor.setContent(faker.lorem().sentence());
                msgFromDoctor.setType(ChatMessage.MessageType.CHAT);
                chatService.sendMessage(msgFromDoctor);

                // Пациент → Врач
                ChatMessage msgFromPatient = new ChatMessage();
                msgFromPatient.setSenderId(patientId);
                msgFromPatient.setReceiverId(doctorId);
                msgFromPatient.setContent(faker.lorem().sentence());
                msgFromPatient.setType(ChatMessage.MessageType.CHAT);
                chatService.sendMessage(msgFromPatient);
            }
        }
    }
}
