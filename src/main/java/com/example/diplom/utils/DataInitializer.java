package com.example.diplom.utils;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.models.*;
import com.example.diplom.repositories.*;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.ChatService;
import com.github.javafaker.Faker;
import io.minio.MinioClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
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
    private final MinioClient minioClient;
    private final AttachmentService attachmentService;
    private final ChatService chatService;
    private final Faker faker;
    private final Faker englishFaker;

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
        this.faker = new Faker(new Locale("ru"));
        this.englishFaker = new Faker(Locale.ENGLISH);
    }

    @PostConstruct
    public void init() {
        populateSpecializations();
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
        String[] specializations = {
                "Стоматология", "Кардиология", "Дерматология",
                "Офтальмология", "Терапия", "Педиатрия"
        };
        for (String name : specializations) {
            Specialization specialization = new Specialization(name);
            specializationRepository.save(specialization);
        }
    }

    private void populateDoctors() {
        List<String> графики = Arrays.asList("будни", "через_день");

        for (int i = 0; i < 10; i++) {
            Doctor doctor = new Doctor();
            doctor.setFullName(faker.name().lastName() + " " + faker.name().firstName());
            doctor.setEmail(englishFaker.internet().emailAddress());
            doctor.setPhone("8" + faker.number().digits(10));
            doctor.setPassword(passwordEncoder.encode("password"));
            doctor.setRole("ROLE_DOCTOR");
            doctor.setUniqueCode(faker.number().digits(7));

            List<Specialization> allSpecs = specializationRepository.findAll();
            doctor.setSpecialization(allSpecs.get(i % allSpecs.size()));

            doctorRepository.save(doctor);
        }
    }

    private void populatePatients() {
        for (int i = 0; i < 100; i++) {
            Patient patient = new Patient();
            patient.setFullName(faker.name().lastName() + " " + faker.name().firstName());
            patient.setBirthDate(faker.date().birthday(18, 80)
                    .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            patient.setEmail(englishFaker.internet().emailAddress()); // только англ
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

        for (Doctor doctor : doctors) {
            Collections.shuffle(patients, random);
            int numberOfConnections = random.nextInt(patients.size()) + 1;
            for (int i = 0; i < numberOfConnections; i++) {
                Patient patient = patients.get(i);
                DoctorPatient doctorPatient = new DoctorPatient(doctor, patient);
                doctorPatientRepository.save(doctorPatient);
                connectedPatientIds.add(patient.getId());
            }
        }

        for (Patient patient : patients) {
            if (!connectedPatientIds.contains(patient.getId())) {
                Doctor randomDoctor = doctors.get(random.nextInt(doctors.size()));
                DoctorPatient doctorPatient = new DoctorPatient(randomDoctor, patient);
                doctorPatientRepository.save(doctorPatient);
            }
        }
    }

    private void populateServices() {
        Map<String, List<String>> услугиПоСпециализации = new HashMap<>();
        услугиПоСпециализации.put("Стоматология", Arrays.asList("Удаление зуба", "Пломбирование", "Профессиональная чистка"));
        услугиПоСпециализации.put("Кардиология", Arrays.asList("ЭКГ", "Консультация кардиолога", "Холтер"));
        услугиПоСпециализации.put("Дерматология", Arrays.asList("Осмотр кожи", "Удаление родинок", "Лечение акне"));
        услугиПоСпециализации.put("Офтальмология", Arrays.asList("Проверка зрения", "Подбор очков", "Фундоскопия"));
        услугиПоСпециализации.put("Терапия", Arrays.asList("Общий приём", "Назначение анализов", "Лечение ОРВИ"));
        услугиПоСпециализации.put("Педиатрия", Arrays.asList("Осмотр ребёнка", "Прививка", "Консультация родителей"));

        for (Doctor doctor : doctorRepository.findAll()) {
            String spec = doctor.getSpecialization().getName();
            List<String> услуги = услугиПоСпециализации.getOrDefault(spec, List.of("Консультация"));
            for (String name : услуги) {
                Service service = new Service();
                service.setName(name);
                service.setPrice(BigDecimal.valueOf(faker.number().numberBetween(500, 5000)));
                service.setDoctor(doctor);
                serviceRepository.save(service);
            }
        }
    }

    private void populateVisits() {
        Map<UUID, List<Patient>> doctorPatientsMap = new HashMap<>();
        for (DoctorPatient dp : doctorPatientRepository.findAll()) {
            doctorPatientsMap
                    .computeIfAbsent(dp.getDoctor().getId(), k -> new ArrayList<>())
                    .add(dp.getPatient());
        }

        LocalDateTime startDate = LocalDateTime.now().minusMonths(4);
        LocalDateTime endDate = LocalDateTime.now();

        List<Doctor> allDoctors = doctorRepository.findAll();

        for (int index = 0; index < allDoctors.size(); index++) {
            Doctor doctor = allDoctors.get(index);
            boolean worksEveryOtherDay = index % 2 == 0;

            List<Patient> linkedPatients = doctorPatientsMap.getOrDefault(doctor.getId(), List.of());
            LocalDateTime currentDate = startDate;

            while (currentDate.isBefore(endDate)) {
                boolean isWorkingDay;

                if (worksEveryOtherDay) {
                    isWorkingDay = (currentDate.toLocalDate().toEpochDay() - startDate.toLocalDate().toEpochDay()) % 2 == 0;
                } else {
                    DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
                    isWorkingDay = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
                }

                if (isWorkingDay) {
                    for (int i = 0; i < 2; i++) {
                        if (linkedPatients.isEmpty()) continue;

                        Patient patient = linkedPatients.get(faker.number().numberBetween(0, linkedPatients.size()));

                        Visit visit = new Visit();
                        visit.setDoctor(doctor);
                        visit.setPatient(patient);
                        visit.setVisitDate(currentDate.plusHours(faker.number().numberBetween(8, 18)));
                        visit.setNotes(generateNotesBySpecialization(doctor.getSpecialization().getName()));
                        visit.setFinished(faker.bool().bool());
                        visit.setTotalCost(BigDecimal.ZERO);
                        visit = visitRepository.save(visit);

                        if (visit.isFinished()) {
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
                        }
                    }
                }

                currentDate = currentDate.plusDays(1);
            }
        }
    }


    private String generateNotesBySpecialization(String specialization) {
        return switch (specialization) {
            case "Стоматология" -> "Жалобы на зубную боль, выполнено пломбирование.";
            case "Кардиология" -> "Повышенное давление, рекомендована ЭКГ и холтер.";
            case "Дерматология" -> "Высыпания на коже, начато лечение акне.";
            case "Офтальмология" -> "Снижение зрения, рекомендованы очки.";
            case "Терапия" -> "Обследование без отклонений, назначены анализы.";
            case "Педиатрия" -> "Плановый осмотр ребёнка, сделана прививка.";
            default -> "Консультация завершена.";
        };
    }

    private void populateAttachments() {
        List<Visit> visits = visitRepository.findAll();
        int counter = 0;

        for (Visit visit : visits) {
            int attachmentCount = faker.number().numberBetween(0, 3);

            for (int i = 0; i < attachmentCount; i++) {
                try {
                    byte[] fileContent;
                    String randomFileName;
                    String contentType;

                    if (counter % 2 == 0) {
                        ByteArrayOutputStream pdfOs = new ByteArrayOutputStream();
                        try (PDDocument document = new PDDocument()) {
                            PDPage page = new PDPage();
                            document.addPage(page);

                            //  1. Загрузка шрифта из ресурсов
                            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("ofont.ru_Futura PT.ttf");
                            if (fontStream == null) throw new RuntimeException("Шрифт не найден в ресурсах");
                            PDType0Font customFont = PDType0Font.load(document, fontStream);

                            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                                cs.setFont(customFont, 14);  // 2. Установка шрифта
                                cs.beginText();
                                cs.newLineAtOffset(100, 700);
                                cs.showText("Медицинский отчёт о визите: " + visit.getId());
                                cs.newLineAtOffset(0, -20);
                                cs.showText("Врач: " + visit.getDoctor().getFullName());
                                cs.newLineAtOffset(0, -20);
                                cs.showText("Пациент: " + visit.getPatient().getFullName());
                                cs.newLineAtOffset(0, -20);
                                cs.showText("Комментарий: " + visit.getNotes());
                                cs.endText();
                            }
                            document.save(pdfOs);
                        }
                        fileContent = pdfOs.toByteArray();
                        randomFileName = UUID.randomUUID() + ".pdf";
                        contentType = "application/pdf";
                    } else {
                        String txt = faker.lorem().paragraph();
                        fileContent = txt.getBytes(StandardCharsets.UTF_8);
                        randomFileName = UUID.randomUUID() + ".txt";
                        contentType = "text/plain";
                    }

                    MockMultipartFile multipartFile = new MockMultipartFile(
                            "file", randomFileName, contentType, fileContent
                    );

                    AddAttachmentRequest req = new AddAttachmentRequest(
                            visit.getId(), multipartFile, faker.lorem().sentence()
                    );

                    attachmentService.addAttachment(visit.getPatient().getId(), req);
                    counter++;

                } catch (Exception e) {
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
        List<DoctorPatient> links = doctorPatientRepository.findAll();

        for (DoctorPatient dp : links) {
            String doctorId = dp.getDoctor().getId().toString();
            String patientId = dp.getPatient().getId().toString();

            int count = faker.number().numberBetween(1, 5);
            for (int i = 0; i < count; i++) {
                ChatMessage msgFromDoctor = new ChatMessage();
                msgFromDoctor.setSenderId(doctorId);
                msgFromDoctor.setReceiverId(patientId);
                msgFromDoctor.setContent("Здравствуйте!");
                msgFromDoctor.setType(ChatMessage.MessageType.CHAT);
                chatService.sendMessage(msgFromDoctor);

                ChatMessage msgFromPatient = new ChatMessage();
                msgFromPatient.setSenderId(patientId);
                msgFromPatient.setReceiverId(doctorId);
                msgFromPatient.setContent("Спасибо, доктор! ");
                msgFromPatient.setType(ChatMessage.MessageType.CHAT);
                chatService.sendMessage(msgFromPatient);
            }
        }
    }
}
