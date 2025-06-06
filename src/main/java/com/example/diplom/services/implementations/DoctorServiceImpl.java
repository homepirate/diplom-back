package com.example.diplom.services.implementations;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.AppointmentWarningException;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.notif.NotificationMailService;
import com.example.diplom.repositories.*;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.DoctorRegistrationDto;
import com.example.diplom.services.dtos.VisitDto;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CacheConfig(cacheNames = "doctorCache")
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
    private final NotificationMailService notificationService;
    private final AttachmentService attachmentService;
    private final DoctorPatientRepository doctorPatientRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public DoctorServiceImpl(
            DoctorRepository doctorRepository,
            VisitRepository visitRepository,
            ServiceRepository serviceRepository,
            SpecializationRepository specializationRepository,
            PatientRepository patientRepository,
            VisitServiceRepository visitServiceRepository,
            PasswordEncoder passwordEncoder,
            ModelMapper modelMapper,
            NotificationMailService notificationService,
            AttachmentService attachmentService,
            DoctorPatientRepository doctorPatientRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
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
        this.doctorPatientRepository = doctorPatientRepository;
        this.redisTemplate = redisTemplate;
    }

    private void evictDoctorCache(UUID doctorId) {
        String cacheName = "doctorCache";
        String pattern = cacheName + "::" + doctorId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void evictPatientCache(UUID patientId) {
        String cacheName = "patientCache";
        String pattern = cacheName + "::" + patientId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

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

        String uniqueCode = String.valueOf(new Random().nextInt(8999999) + 1000000);
        doctorDto.setUniqueCode(uniqueCode);

        doctorRepository.save(modelMapper.map(doctorDto, Doctor.class));
    }

    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    @Cacheable(key = "#doctorId + ':' + #root.methodName + ':' + #month + ':' + #year")
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
    @Cacheable(key = "#doctorId + ':' + #root.methodName + ':' + #date")
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

    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public void createServiceForDoctor(UUID doctorId, CreateServiceRequest serviceRequest) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        Optional<com.example.diplom.models.Service> existing = serviceRepository.findByDoctorIdAndName(doctorId, serviceRequest.name());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Service with that name already exists.");
        }

        com.example.diplom.models.Service newService = new com.example.diplom.models.Service();
        newService.setName(serviceRequest.name());
        newService.setPrice(serviceRequest.price());
        newService.setDoctor(doctor);

        serviceRepository.save(newService);
        evictDoctorCache(doctorId);

    }

    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    @Cacheable(key = "#doctorId + ':' + #root.methodName")
    public List<ServiceResponse> getDoctorServices(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));

        return serviceRepository.findByDoctorId(doctorId)
                .stream()
                .map(s -> new ServiceResponse(s.getName(), s.getPrice()))
                .toList();
    }

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
        evictDoctorCache(doctorId);

    }

    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    @Cacheable(key = "#doctorId + ':' + #root.methodName")
    public List<PatientResponse> getDoctorPatients(UUID doctorId) {

        List<Patient> patients = doctorPatientRepository.findPatientsByDoctorId(doctorId);

        return patients
                .stream().map(patient -> new PatientResponse(
                        patient.getFullName(),
                        patient.getBirthDate(),
                        patient.getId()
                ))
                .toList();
    }

    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #visitIdRequest.id())")
    public void cancelVisit(UUID doctorId, VisitIdRequest visitIdRequest) {
        if (!visitRepository.existsById(visitIdRequest.id())) {
            throw new ResourceNotFoundException("Visit not found with id " + visitIdRequest.id());
        }
        Visit visit = visitRepository.findById(visitIdRequest.id())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        visitRepository.deleteById(visitIdRequest.id());
        evictDoctorCache(doctorId);
        evictPatientCache(visit.getPatient().getId());
    }


    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #finishVisitRequest.id())")
    public void finishVisit(UUID doctorId, FinishVisitRequest finishVisitRequest) {

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
        evictDoctorCache(doctorId);
        evictPatientCache(visit.getPatient().getId());

    }

    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #visitIdRequest.id())")
    @Cacheable(key = "#doctorId + ':' + #root.methodName + ':' + #visitIdRequest.id()")
    public VisitDetailsResponse getFinishVisitData(UUID doctorId, VisitIdRequest visitIdRequest) {
        Visit visit = visitRepository.findById(visitIdRequest.id())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found " + visitIdRequest.id()));
        return toVisitDetailsResponse(visit);
    }


    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorPatientOwnership(authentication, #doctorId, #patientId)")
    @Cacheable(key = "#doctorId + ':' + #root.methodName + ':' + #patientId")
    public PatientMedCardResponse getPatientMedicalCard(UUID doctorId, UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found " + patientId));

        List<Visit> visits = visitRepository.findByPatientIdAndDoctorId(patientId, doctorId);
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
        evictPatientCache(visit.getPatient().getId());

    }

    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorPatientOwnership(authentication, #doctorId, #visitRequest.patientId())")
    public CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest) {
        AppointmentCheckResult result = checkAppointmentOverlap(doctorId, visitRequest.visitDate(), null);
        if (result == AppointmentCheckResult.ERROR) {
            throw new IllegalArgumentException("Записи пересекаются");
        } else if (result == AppointmentCheckResult.WARNING && !visitRequest.force()) {
            throw new AppointmentWarningException("There is another appointment close to this time. Is everything right?");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found " + doctorId));
        Patient patient = patientRepository.findById(visitRequest.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found " + visitRequest.patientId()));
        if(patient.getPhone().contains("удален")){
            throw new ResourceNotFoundException("Пациент был удален");
        }
        Visit visit = new Visit();
        visit.setDoctor(doctor);
        visit.setPatient(patient);
        visit.setVisitDate(visitRequest.visitDate());
        visit.setNotes(visitRequest.notes());
        visit.setFinished(false);
        visit.setTotalCost(BigDecimal.ZERO);

        Visit saved = visitRepository.save(visit);
        notificationService.sendVisitCreatedNotification(patient.getEmail(), saved.getVisitDate().toString());
        evictDoctorCache(doctorId);
        evictPatientCache(visit.getPatient().getId());

        return new CreateVisitResponse(saved.getVisitDate(), saved.getId());
    }

    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #rearrangeRequest.visitId())")
    public void rearrangeVisit(UUID doctorId, RearrangeVisitRequest rearrangeRequest) {
        AppointmentCheckResult result = checkAppointmentOverlap(doctorId, rearrangeRequest.newVisitDate(), rearrangeRequest.visitId());
        if (result == AppointmentCheckResult.ERROR) {
            throw new IllegalArgumentException("Appointments overlap");
        } else if (result == AppointmentCheckResult.WARNING && !rearrangeRequest.force()) {
            throw new AppointmentWarningException("There is another appointment close to this time. Is everything right?");
        }

        Visit visit = visitRepository.findById(rearrangeRequest.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found " + rearrangeRequest.visitId()));

        visit.setVisitDate(rearrangeRequest.newVisitDate());
        visitRepository.save(visit);
        evictDoctorCache(doctorId);
        evictPatientCache(visit.getPatient().getId());
        notificationService.sendVisitCreatedNotification(visit.getPatient().getEmail(), visit.getVisitDate().toString());
    }

    private AppointmentCheckResult checkAppointmentOverlap(UUID doctorId, LocalDateTime newTime, UUID existingVisitId) {
        LocalDateTime windowStart = newTime.minusMinutes(15);
        LocalDateTime windowEnd = newTime.plusMinutes(15);

        List<Visit> nearbyVisits = visitRepository.findByDoctorIdAndVisitDateBetween(doctorId, windowStart, windowEnd);
        for (Visit visit : nearbyVisits) {
            if (existingVisitId != null && visit.getId().equals(existingVisitId)) {
                continue;
            }
            long diff = Math.abs(java.time.Duration.between(newTime, visit.getVisitDate()).toMinutes());
            if (diff == 0) {
                return AppointmentCheckResult.ERROR;
            } else if (diff < 15) {
                return AppointmentCheckResult.WARNING;
            }
        }
        return AppointmentCheckResult.OK;
    }



    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public byte[] generateFinancialDashboardReport(UUID doctorId, ReportRequest reportRequest) {

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd.MM");
        DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        LocalDateTime startOfDay = reportRequest.startDate().atStartOfDay();
        LocalDateTime endOfDay   = reportRequest.endDate().atTime(LocalTime.MAX);

        List<Visit> visits = visitRepository.findByDoctorIdAndVisitDateBetween(
                doctorId,
                startOfDay,
                endOfDay
        );
        if (visits.isEmpty()) {
            throw new ResourceNotFoundException("В указанный период визиты не найдены.");
        }

        BigDecimal totalRevenue = visits.stream()
                .map(Visit::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int visitCount = visits.size();

        Map<String, BigDecimal> serviceRevenue = new HashMap<>();
        for (Visit visit : visits) {
            List<VisitService> vsList = visitServiceRepository.findByVisit(visit);
            for (VisitService vs : vsList) {
                String serviceName = vs.getService().getName();
                BigDecimal revenue = vs.getService().getPrice()
                        .multiply(BigDecimal.valueOf(vs.getQuantity()));
                serviceRevenue.put(serviceName,
                        serviceRevenue.getOrDefault(serviceName, BigDecimal.ZERO).add(revenue));
            }
        }

        LocalDate currentDate = reportRequest.startDate();
        LocalDate endDate = reportRequest.endDate();

        Map<LocalDate, BigDecimal> revenuePerDay = new TreeMap<>();
        while (!currentDate.isAfter(endDate)) {
            revenuePerDay.put(currentDate, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }


        for (Visit visit : visits) {
            LocalDate day = visit.getVisitDate().toLocalDate();
            BigDecimal currentRevenue = revenuePerDay.get(day);
            revenuePerDay.put(day, currentRevenue.add(visit.getTotalCost()));
        }
        DefaultCategoryDataset dayDataset = new DefaultCategoryDataset();
        for (Map.Entry<LocalDate, BigDecimal> entry : revenuePerDay.entrySet()) {
            dayDataset.addValue(entry.getValue(), "Выручка", entry.getKey().format(dayFormatter));
        }
        JFreeChart barChart = ChartFactory.createBarChart(
                "Ежедневная выручка",
                "",
                "Выручка",
                dayDataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false
        );
        CategoryPlot barPlot = barChart.getCategoryPlot();
        barPlot.getDomainAxis().setTickLabelsVisible(false);

        ByteArrayOutputStream barChartOut = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(barChartOut, barChart, 400, 300);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации столбцовой диаграммы ежедневной выручки", e);
        }
        byte[] barChartImageBytes = barChartOut.toByteArray();

        DefaultPieDataset pieDataset = new DefaultPieDataset();
        for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Процентное соотношение выручки по услугам",
                pieDataset,
                false,
                true,
                false
        );
        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}"));

        TextTitle totalRevenueSubtitle = new TextTitle("Общая выручка: " + totalRevenue);
        totalRevenueSubtitle.setPosition(RectangleEdge.BOTTOM);
        totalRevenueSubtitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
        pieChart.addSubtitle(totalRevenueSubtitle);

        ByteArrayOutputStream pieChartOut = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(pieChartOut, pieChart, 400, 300);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации круговой диаграммы для выручки по услугам", e);
        }
        byte[] pieChartImageBytes = pieChartOut.toByteArray();

        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(pdfOut);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont(
                    "src/main/resources/ofont.ru_Futura PT.ttf",
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED,
                    true
            );
            document.setFont(font);

            document.add(new Paragraph("Финансовый отчёт за период: "
                    + reportRequest.startDate().format(fullDateFormatter) + " - "
                    + reportRequest.endDate().format(fullDateFormatter))
                    .setBold()
                    .setFontSize(16));

            document.add(new Paragraph("Общее количество визитов: " + visitCount));
            document.add(new Paragraph("Общая выручка: " + totalRevenue));

            Image barChartImage = new Image(ImageDataFactory.create(barChartImageBytes));
            barChartImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(barChartImage);

            Image pieChartImage = new Image(ImageDataFactory.create(pieChartImageBytes));
            pieChartImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

            document.add(pieChartImage);

            document.add(new Paragraph("Услуги:").setBold());
            float[] serviceColumnWidths = {20F, 480F};
            Table serviceTable = new Table(UnitValue.createPointArray(serviceColumnWidths))
                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(serviceTable);

            for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
                java.awt.Paint paint = piePlot.getSectionPaint(entry.getKey());
                java.awt.Color awtColor = (paint instanceof java.awt.Color) ? (java.awt.Color) paint : java.awt.Color.BLACK;
                DeviceRgb iTextColor = new DeviceRgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

                Cell markerCell = new Cell()
                        .add(new Paragraph(" "))
                        .setBackgroundColor(iTextColor)
                        .setWidth(20)
                        .setHeight(20)
                        .setBorder(Border.NO_BORDER);

                Cell textCell = new Cell()
                        .add(new Paragraph("Услуга: " + entry.getKey() + " — выручка: " + entry.getValue()))
                        .setBorder(Border.NO_BORDER);

                serviceTable.addCell(markerCell);
                serviceTable.addCell(textCell);
            }
            document.add(serviceTable);

            Map<String, List<Visit>> visitsByPatient = visits.stream()
                    .collect(Collectors.groupingBy(v -> {
                        if (v.getPatient() != null) {
                            return v.getPatient().getFullName();
                        } else {
                            return "Неизвестно";
                        }
                    }));

            List<PatientStats> patientStatsList = new ArrayList<>();
            for (Map.Entry<String, List<Visit>> entry : visitsByPatient.entrySet()) {
                String patientName = entry.getKey();
                List<Visit> patientVisits = entry.getValue();

                int count = patientVisits.size();

                BigDecimal totalForPatient = patientVisits.stream()
                        .map(Visit::getTotalCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                patientStatsList.add(new PatientStats(patientName, count, totalForPatient));
            }

            patientStatsList.sort(Comparator.comparing(PatientStats::patientName));

            document.add(new Paragraph("Сводная информация по пациентам:").setBold());
            float[] patientColumnWidths = {200F, 100F, 100F};
            Table patientTable = new Table(UnitValue.createPointArray(patientColumnWidths));

            patientTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

            patientTable.addHeaderCell(new Cell().add(new Paragraph("ФИО пациента").setBold()));
            patientTable.addHeaderCell(new Cell().add(new Paragraph("Кол-во визитов").setBold()));
            patientTable.addHeaderCell(new Cell().add(new Paragraph("Общая выручка").setBold()));

            for (PatientStats stats : patientStatsList) {
                patientTable.addCell(new Cell().add(new Paragraph(stats.patientName())));
                patientTable.addCell(new Cell().add(new Paragraph(String.valueOf(stats.visitCount()))));
                patientTable.addCell(new Cell().add(new Paragraph(stats.totalRevenue().toString())));
            }
            document.add(patientTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации PDF отчёта", e);
        }

        return pdfOut.toByteArray();
    }

    public record PatientStats(String patientName, int visitCount, BigDecimal totalRevenue) {}

    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public PatientResponse addPatientManually(UUID doctorId, AddPatientRequest addPatientRequest) {
        Patient patient = new Patient();
        patient.setFullName(addPatientRequest.fullName());
        patient.setPhone(addPatientRequest.phone());
        patient.setBirthDate(addPatientRequest.birthDate());
        patient.setIsTemporary(true);
        Patient savedPatient = patientRepository.save(patient);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));

        DoctorPatient dp = new DoctorPatient();
        dp.setDoctor(doctor);
        dp.setPatient(savedPatient);
        doctorPatientRepository.save(dp);
        evictDoctorCache(doctorId);

        return new PatientResponse(savedPatient.getFullName(), savedPatient.getBirthDate(), savedPatient.getId());
    }


    private VisitDetailsResponse toVisitDetailsResponse(Visit visit) {
        UUID doctorId = visit.getDoctor().getId();
        List<com.example.diplom.models.Service> docServices =
                serviceRepository.findByDoctorId(doctorId);

        Map<UUID, Integer> serviceQuantities = visitServiceRepository.findByVisit(visit).stream()
                .collect(Collectors.toMap(
                        vs -> vs.getService().getId(),
                        VisitService::getQuantity
                ));

        List<VisitServicesDetailsResponse> services = docServices.stream()
                .map(s -> new VisitServicesDetailsResponse(
                        s.getId(),
                        s.getName(),
                        s.getPrice(),
                        serviceQuantities.getOrDefault(s.getId(), 0)
                ))
                .toList();

        List<String> attachments = buildAttachmentUrls(visit.getAttachments());

        return new VisitDetailsResponse(
                visit.getId(),
                visit.getVisitDate(),
                visit.isFinished(),
                Optional.ofNullable(visit.getNotes()).orElse(""),
                visit.getTotalCost(),
                services,
                attachments
        );
    }


    private List<String> buildAttachmentUrls(Collection<Attachment> attachments) {
        return attachments.stream()
                .map(a -> {
                    try {
                        return attachmentService.getPresignedUrlForAttachment(a.getId());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}