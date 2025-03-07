package com.example.diplom.services.implementations;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.AppointmentWarningException;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.notif.NotificationService;
import com.example.diplom.repositories.*;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.DoctorRegistrationDto;
import com.example.diplom.services.dtos.VisitDto;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
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
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final DoctorPatientRepository doctorPatientRepository;

    @Autowired
    public DoctorServiceImpl(DoctorRepository doctorRepository, VisitRepository visitRepository, ServiceRepository serviceRepository, SpecializationRepository specializationRepository, PatientRepository patientRepository, VisitServiceRepository visitServiceRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper, NotificationService notificationService, AttachmentService attachmentService, DoctorPatientRepository doctorPatientRepository) {
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

        List<Patient> patients = doctorPatientRepository.findPatientsByDoctorId(doctorId);

        return patients
                .stream().map(patient -> new PatientResponse(
                        patient.getFullName(),
                        patient.getBirthDate(),
                        patient.getId()
                ))
                .toList();
    }

    // -------------------------------------------------
    // CREATE VISIT
    // -------------------------------------------------

    // -------------------------------------------------
    // CANCEL VISIT
    // -------------------------------------------------

    /**
     * Notice that your cancelVisit method doesn't currently accept the doctorId
     * as a parameter. So either add it or you can do a custom PreAuthorize check
     * that extracts the docId from the JWT inside doctorAuthz.
     * <p>
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
    public void finishVisit(UUID doctorId, FinishVisitRequest finishVisitRequest) {
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

    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorPatientOwnership(authentication, #doctorId, #visitRequest.patientId())")

    public CreateVisitResponse createVisit(UUID doctorId, CreateVisitRequest visitRequest) {
        // Check for overlapping appointments
        AppointmentCheckResult result = checkAppointmentOverlap(doctorId, visitRequest.visitDate(), null);
        if (result == AppointmentCheckResult.ERROR) {
            throw new IllegalArgumentException("Appointments overlap");
        } else if (result == AppointmentCheckResult.WARNING && !visitRequest.force()) {
            throw new AppointmentWarningException("There is another appointment close to this time. Is everything right?");
        }

        // ... existing logic to create visit
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

    @Override
    @PreAuthorize("@doctorAuthz.hasDoctorVisitOwnership(authentication, #doctorId, #rearrangeRequest.visitId())")
    public void rearrangeVisit(UUID doctorId, RearrangeVisitRequest rearrangeRequest) {
        // Check for overlapping appointments (exclude the visit being rearranged)
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
        notificationService.sendVisitCreatedNotification(visit.getPatient().getEmail(), visit.getVisitDate().toString());
    }

    // Helper method to check for overlapping appointments
    private AppointmentCheckResult checkAppointmentOverlap(UUID doctorId, LocalDateTime newTime, UUID existingVisitId) {
        LocalDateTime windowStart = newTime.minusMinutes(15);
        LocalDateTime windowEnd = newTime.plusMinutes(15);

        List<Visit> nearbyVisits = visitRepository.findByDoctorIdAndVisitDateBetween(doctorId, windowStart, windowEnd);
        for (Visit visit : nearbyVisits) {
            // When rearranging, ignore the current visit
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


    /**
     * Генерация финансового отчёта с дашбордом в формате PDF для указанного доктора за выбранный период.
     * Отчёт включает сводные данные по визитам и график распределения выручки по услугам.
     *
     * @param doctorId  идентификатор доктора
     * @param reportRequest период
     * @return PDF отчёт в виде массива байтов
     */
    @Override
    @PreAuthorize("@doctorAuthz.matchDoctorId(authentication, #doctorId)")
    public byte[] generateFinancialDashboardReport(UUID doctorId, ReportRequest reportRequest) {
        // Define date formatters
        DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd.MM");

        // Step 1: Retrieve visits for the specified period
        List<Visit> visits = visitRepository.findByDoctorIdAndVisitDateBetween(
                doctorId, reportRequest.startDate(), reportRequest.endDate());
        if (visits.isEmpty()) {
            throw new ResourceNotFoundException("No visits found for the specified period.");
        }
        BigDecimal totalRevenue = visits.stream()
                .map(Visit::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int visitCount = visits.size();

        // Step 2: Aggregate service revenue: compute total revenue per service
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

        // Step 3: Calculate daily revenue for the line chart
        Map<LocalDate, BigDecimal> revenuePerDay = new TreeMap<>();
        for (Visit visit : visits) {
            LocalDate day = visit.getVisitDate().toLocalDate();
            BigDecimal currentRevenue = revenuePerDay.getOrDefault(day, BigDecimal.ZERO);
            revenuePerDay.put(day, currentRevenue.add(visit.getTotalCost()));
        }
        // Create a line chart for daily revenue (dates formatted as dd.MM)
        DefaultCategoryDataset dayDataset = new DefaultCategoryDataset();
        for (Map.Entry<LocalDate, BigDecimal> entry : revenuePerDay.entrySet()) {
            dayDataset.addValue(entry.getValue(), "Revenue", entry.getKey().format(dayFormatter));
        }
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Daily Revenue",
                "Date",
                "Revenue",
                dayDataset
        );
        ByteArrayOutputStream lineChartOut = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(lineChartOut, lineChart, 500, 400);
        } catch (Exception e) {
            throw new RuntimeException("Error generating daily revenue line chart", e);
        }
        byte[] lineChartImageBytes = lineChartOut.toByteArray();

        // Step 4: Create a pie chart for service revenue percentages
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Service Revenue Percentage",
                pieDataset,
                true,  // include legend
                true,  // tooltips
                false  // URLs
        );
        // Set label generator to display percentages
        PiePlot piePlot = (PiePlot) pieChart.getPlot();
        piePlot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {2}"));
        ByteArrayOutputStream pieChartOut = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(pieChartOut, pieChart, 500, 400);
        } catch (Exception e) {
            throw new RuntimeException("Error generating pie chart for service revenue", e);
        }
        byte[] pieChartImageBytes = pieChartOut.toByteArray();

        // Step 5: Generate PDF report using iText
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(pdfOut);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Report header: period (formatted as DD.MM.YYYY)
            document.add(new Paragraph("Financial Report for the period: "
                    + reportRequest.startDate().format(fullDateFormatter) + " - "
                    + reportRequest.endDate().format(fullDateFormatter))
                    .setBold()
                    .setFontSize(16));
            document.add(new Paragraph(" ")); // Spacing

            // Summary: total number of visits and total revenue
            document.add(new Paragraph("Total number of visits: " + visitCount));
            document.add(new Paragraph("Total revenue: " + totalRevenue));
            document.add(new Paragraph(" "));

            // Numbered list of visits with date (formatted as DD.MM.YYYY), patient's full name, and revenue per visit
            document.add(new Paragraph("Visits:").setBold());
            int index = 1;
            for (Visit visit : visits) {
                String patientName = (visit.getPatient() != null)
                        ? visit.getPatient().getFullName() : "Unknown";
                document.add(new Paragraph(index++ + ". Visit on "
                        + visit.getVisitDate().format(fullDateFormatter)
                        + " - Patient: " + patientName
                        + " - Revenue: " + visit.getTotalCost()));
            }
            document.add(new Paragraph(" "));

            // Insert line chart: daily revenue chart
            document.add(new Paragraph("Daily Revenue Line Chart:").setBold());
            Image lineChartImage = new Image(ImageDataFactory.create(lineChartImageBytes));
            document.add(lineChartImage);
            document.add(new Paragraph(" "));

            // Insert pie chart: service revenue percentage chart
            document.add(new Paragraph("Service Revenue Percentage Pie Chart:").setBold());
            Image pieChartImage = new Image(ImageDataFactory.create(pieChartImageBytes));
            document.add(pieChartImage);
            document.add(new Paragraph(" "));

            // List of services: each service with its total revenue and a color marker matching the pie chart section.
            document.add(new Paragraph("Services:").setBold());
            // Create a table with two columns: one for the color marker, one for the service text.
            float[] columnWidths = {20F, 480F};
            Table serviceTable = new Table(UnitValue.createPointArray(columnWidths));
            for (Map.Entry<String, BigDecimal> entry : serviceRevenue.entrySet()) {
                // Get the color from the pie chart for the service.
                java.awt.Paint paint = piePlot.getSectionPaint(entry.getKey());
                java.awt.Color awtColor = (paint instanceof java.awt.Color) ? (java.awt.Color) paint : java.awt.Color.BLACK;
                DeviceRgb iTextColor = new DeviceRgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

                // Create a cell with a colored rectangle.
                Cell markerCell = new Cell()
                        .add(new Paragraph(" "))
                        .setBackgroundColor(iTextColor)
                        .setWidth(20)
                        .setHeight(20)
                        .setBorder(Border.NO_BORDER);
                // Create a cell with the service text.
                Cell textCell = new Cell()
                        .add(new Paragraph("Service: " + entry.getKey() + " - Total revenue: " + entry.getValue()))
                        .setBorder(Border.NO_BORDER);
                serviceTable.addCell(markerCell);
                serviceTable.addCell(textCell);
            }
            document.add(serviceTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }

        return pdfOut.toByteArray();
    }

}