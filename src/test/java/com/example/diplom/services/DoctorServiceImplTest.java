// src/test/java/com/example/diplom/services/implementations/DoctorServiceImplTest.java
package com.example.diplom.services;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.*;
import com.example.diplom.models.*;
import com.example.diplom.notif.NotificationMailService;
import com.example.diplom.repositories.*;
import com.example.diplom.services.dtos.VisitDto;
import com.example.diplom.services.implementations.DoctorServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.diplom.services.dtos.DoctorRegistrationDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)

class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private SpecializationRepository specializationRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private VisitServiceRepository visitServiceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private NotificationMailService notificationService;
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private DoctorPatientRepository doctorPatientRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private DoctorServiceImpl service;

    private UUID doctorId;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        doctor = new Doctor();
        doctor.setId(doctorId);
        lenient().when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
    }


    @Test
    void registerDoctor_whenSpecializationNotFound_thenThrow() {
        when(specializationRepository.findByName("Spec"))
                .thenReturn(Optional.empty());
        DoctorRegisterRequest req = new DoctorRegisterRequest("email@email.com", "pass", "89999999999", "phone","Spec");
        assertThrows(IllegalArgumentException.class,
                () -> service.registerDoctor(req));
    }

    @Test
    void registerDoctor_success() {
        Specialization spec = new Specialization("Spec");
        when(specializationRepository.findByName("Spec")).thenReturn(Optional.of(spec));
        when(passwordEncoder.encode("pass")).thenReturn("encPass");
        Doctor mapped = new Doctor();
        when(modelMapper.map(any(), eq(Doctor.class))).thenReturn(mapped);

        service.registerDoctor(new DoctorRegisterRequest("email@email.com", "pass", "89999999999", "phone","Spec"));

        ArgumentCaptor<DoctorRegistrationDto> dtoCaptor = ArgumentCaptor.forClass(DoctorRegistrationDto.class);
        verify(modelMapper).map(dtoCaptor.capture(), eq(Doctor.class));
        DoctorRegistrationDto dto = dtoCaptor.getValue();
        assertEquals("encPass", dto.getPassword());
        assertNotNull(dto.getUniqueCode());
        verify(doctorRepository).save(mapped);
    }

    @Test
    void getDoctorVisitDates_whenDoctorNotFound_thenThrow() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getDoctorVisitDates(doctorId, 1, 2025));
    }

    @Test
    void getDoctorVisitDates_returnsMappedDto() {
        Visit v = new Visit();
        v.setFinished(true);
        v.setTotalCost(BigDecimal.valueOf(123));
        v.setNotes("note");
        List<Visit> visits = List.of(v);
        when(visitRepository.findByDoctorIdAndMonthYear(doctorId, 1, 2025))
                .thenReturn(visits);
        VisitDto dto = new VisitDto();
        when(modelMapper.map(v, VisitDto.class)).thenReturn(dto);

        List<VisitDto> result = service.getDoctorVisitDates(doctorId, 1, 2025);

        assertSame(dto, result.get(0));
        assertTrue(dto.isFinished());
        assertEquals(BigDecimal.valueOf(123), dto.getTotalCost());
        assertEquals("note", dto.getNotes());
    }

    @Test
    void getDoctorVisitDatesByDay_whenDoctorNotFound_thenThrow() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getDoctorVisitDatesByDay(doctorId, "2025-01-01"));
    }

    @Test
    void getDoctorVisitDatesByDay_returnsMappedDto() {
        Visit v = new Visit();
        v.setFinished(false);
        v.setTotalCost(BigDecimal.valueOf(50));
        v.setNotes("dayNote");
        List<Visit> visits = List.of(v);
        when(visitRepository.findByDoctorIdAndDate(doctorId, "2025-01-01"))
                .thenReturn(visits);
        VisitDto dto = new VisitDto();
        when(modelMapper.map(v, VisitDto.class)).thenReturn(dto);

        List<VisitDto> result = service.getDoctorVisitDatesByDay(doctorId, "2025-01-01");

        assertSame(dto, result.get(0));
        assertFalse(dto.isFinished());
        assertEquals("dayNote", dto.getNotes());
    }

    @Test
    void createServiceForDoctor_whenNameExists_thenThrow() {
        when(serviceRepository.findByDoctorIdAndName(doctorId, "X-ray"))
                .thenReturn(Optional.of(new com.example.diplom.models.Service()));
        assertThrows(IllegalArgumentException.class,
                () -> service.createServiceForDoctor(doctorId, new CreateServiceRequest("X-ray", BigDecimal.ONE)));
    }

    @Test
    void createServiceForDoctor_success() {
        when(serviceRepository.findByDoctorIdAndName(doctorId, "US")).thenReturn(Optional.empty());
        service.createServiceForDoctor(doctorId, new CreateServiceRequest("US", BigDecimal.valueOf(10)));
        ArgumentCaptor<com.example.diplom.models.Service> cap = ArgumentCaptor.forClass(com.example.diplom.models.Service.class);
        verify(serviceRepository).save(cap.capture());
        assertEquals("US", cap.getValue().getName());
        assertEquals(BigDecimal.valueOf(10), cap.getValue().getPrice());
    }

    @Test
    void getDoctorServices_whenDoctorNotFound_thenThrow() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getDoctorServices(doctorId));
    }

    @Test
    void getDoctorServices_returnsServices() {
        com.example.diplom.models.Service s = new com.example.diplom.models.Service();
        s.setName("N");
        s.setPrice(BigDecimal.TEN);
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByDoctorId(doctorId)).thenReturn(List.of(s));
        List<ServiceResponse> res = service.getDoctorServices(doctorId);
        assertEquals(1, res.size());
        assertEquals("N", res.get(0).name());
    }

    @Test
    void updateServicePrice_whenPriceNegative_thenThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateServicePrice(doctorId, new UpdateServiceRequest("N", BigDecimal.valueOf(-1))));
    }

    @Test
    void updateServicePrice_whenServiceNotFound_thenThrow() {
        when(serviceRepository.findByDoctorIdAndName(doctorId, "X")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateServicePrice(doctorId, new UpdateServiceRequest("X", BigDecimal.ONE)));
    }

    @Test
    void updateServicePrice_success() {
        com.example.diplom.models.Service s = new com.example.diplom.models.Service();
        when(serviceRepository.findByDoctorIdAndName(doctorId, "X")).thenReturn(Optional.of(s));
        service.updateServicePrice(doctorId, new UpdateServiceRequest("X", BigDecimal.valueOf(20)));
        assertEquals(BigDecimal.valueOf(20), s.getPrice());
        verify(serviceRepository).save(s);
    }

    @Test
    void getDoctorPatients_returnsPatients() {
        Patient p = new Patient();
        p.setId(UUID.randomUUID());
        p.setFullName("P");
        p.setBirthDate(null);
        when(doctorPatientRepository.findPatientsByDoctorId(doctorId)).thenReturn(List.of(p));
        List<PatientResponse> list = service.getDoctorPatients(doctorId);
        assertEquals(1, list.size());
        assertEquals("P", list.get(0).fullName());
    }

    @Test
    void cancelVisit_whenNotFound_thenThrow() {
        when(visitRepository.existsById(any())).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> service.cancelVisit(doctorId, new VisitIdRequest(UUID.randomUUID())));
    }

    @Test
    void cancelVisit_success() {
        UUID vid = UUID.randomUUID();
        when(visitRepository.existsById(vid)).thenReturn(true);
        service.cancelVisit(doctorId, new VisitIdRequest(vid));
        verify(visitRepository).deleteById(vid);
    }

    @Test
    void finishVisit_whenVisitNotFound_thenThrow() {
        when(visitRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.finishVisit(doctorId, new FinishVisitRequest(UUID.randomUUID(), List.of(), "")));
    }

    @Test
    void finishVisit_whenNoServices_thenThrow() {
        Visit v = new Visit();
        v.setId(UUID.randomUUID());
        when(visitRepository.findById(v.getId())).thenReturn(Optional.of(v));
        assertThrows(IllegalArgumentException.class,
                () -> service.finishVisit(doctorId, new FinishVisitRequest(v.getId(), List.of(), "")));
    }

    @Test
    void finishVisit_success() {
        UUID vid = UUID.randomUUID();
        Visit v = new Visit();
        v.setId(vid);
        v.setFinished(false);
        v.setNotes("old");
        v.setDoctor(doctor);
        v.setTotalCost(BigDecimal.ZERO);
        when(visitRepository.findById(vid)).thenReturn(Optional.of(v));
        List<ServiceUpdateRequest> updates = List.of(new ServiceUpdateRequest("Svc", 2));
        com.example.diplom.models.Service msvc = new com.example.diplom.models.Service();
        msvc.setPrice(BigDecimal.valueOf(50));
        when(serviceRepository.findByDoctorIdAndName(doctorId, "Svc")).thenReturn(Optional.of(msvc));
        VisitService vs = new VisitService();
        vs.setService(msvc);
        vs.setQuantity(2);
        when(visitServiceRepository.findByVisit(v)).thenReturn(Collections.emptyList(), List.of(vs));

        service.finishVisit(doctorId, new FinishVisitRequest(vid, updates, "new"));

        assertTrue(v.isFinished());
        assertEquals("new", v.getNotes());
        assertEquals(BigDecimal.valueOf(100), v.getTotalCost());
        verify(visitRepository).save(v);
    }

    @Test
    void getFinishVisitData_success() throws Exception {
        UUID vid = UUID.randomUUID();
        Visit v = new Visit();
        v.setId(vid);
        v.setVisitDate(LocalDateTime.now());
        v.setFinished(true);
        v.setNotes("n");
        v.setTotalCost(BigDecimal.ONE);

        when(visitRepository.findById(vid)).thenReturn(Optional.of(v));
        com.example.diplom.models.Service s1 = new com.example.diplom.models.Service();
        s1.setId(UUID.randomUUID());
        s1.setName("n");
        s1.setPrice(BigDecimal.ONE);
        when(serviceRepository.findByDoctorId(doctorId)).thenReturn(List.of(s1));
        VisitService vs = new VisitService();
        vs.setService(s1);
        vs.setQuantity(3);
        when(visitServiceRepository.findByVisit(v)).thenReturn(List.of(vs));
        when(attachmentService.getPresignedUrlForAttachment(any())).thenReturn("url");

        VisitDetailsResponse res = service.getFinishVisitData(doctorId, new VisitIdRequest(vid));

        assertEquals(vid, res.visitId());
        assertEquals(3, res.services().get(0).quantity());

    }

    @Test
    void createVisit_andOverlapLogic() {
        LocalDateTime now = LocalDateTime.now();
        when(visitRepository.findByDoctorIdAndVisitDateBetween(eq(doctorId), any(), any())).thenReturn(Collections.emptyList());
        Patient p = new Patient();
        p.setId(UUID.randomUUID());
        p.setEmail("e");
        p.setPhone("123");
        when(patientRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(visitRepository.save(any())).thenAnswer(invocation -> {
            Visit v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        CreateVisitResponse resp = service.createVisit(doctorId, new CreateVisitRequest(p.getId(), now, "", false));
        assertNotNull(resp.visitId());
        verify(notificationService).sendVisitCreatedNotification(eq("e"), anyString());
    }

    @Test
    void rearrangeVisit_andOverlapLogic() {
        UUID vid = UUID.randomUUID();
        Visit existing = new Visit();
        existing.setId(vid);
        existing.setPatient(new Patient());
        existing.setDoctor(doctor);
        existing.setVisitDate(LocalDateTime.now());
        when(visitRepository.findById(vid)).thenReturn(Optional.of(existing));
        when(visitRepository.findByDoctorIdAndVisitDateBetween(eq(doctorId), any(), any())).thenReturn(Collections.emptyList());
        service.rearrangeVisit(doctorId, new RearrangeVisitRequest(vid, LocalDateTime.now(), false));
        verify(visitRepository).save(existing);
        verify(notificationService).sendVisitCreatedNotification(any(), any());
    }

    @Test
    void getPatientMedicalCard_success() throws Exception {
        UUID pid = UUID.randomUUID();
        Patient p = new Patient();
        p.setId(pid);
        p.setFullName("FN");
        p.setBirthDate(LocalDate.now());
        p.setEmail("e");
        p.setPhone("ph");
        when(patientRepository.findById(pid)).thenReturn(Optional.of(p));
        Visit v = new Visit();
        v.setId(UUID.randomUUID());
        v.setVisitDate(LocalDateTime.now());
        v.setFinished(false);
        v.setNotes("nm");
        v.setTotalCost(BigDecimal.ONE);
        v.setPatient(p);
        v.setDoctor(doctor);
        when(visitRepository.findByPatientIdAndDoctorId(pid, doctorId)).thenReturn(List.of(v));
        VisitService vs = new VisitService();
        vs.setServiceId(UUID.randomUUID());
        vs.setService(new com.example.diplom.models.Service());
        vs.setQuantity(1);
        when(visitServiceRepository.findByVisit(v)).thenReturn(List.of(vs));
        when(attachmentService.getPresignedUrlForAttachment(any())).thenReturn("u");

        PatientMedCardResponse out = service.getPatientMedicalCard(doctorId, pid);
        assertEquals("FN", out.fullName());
        assertEquals(1, out.visits().size());
    }

    @Test
    void addPatientManually_success() {
        AddPatientRequest req = new AddPatientRequest("n", "ph", LocalDate.now());
        Patient p = new Patient();
        p.setId(UUID.randomUUID());
        p.setFullName("n");
        p.setBirthDate(req.birthDate());
        when(patientRepository.save(any())).thenReturn(p);
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        PatientResponse res = service.addPatientManually(doctorId, req);
        assertEquals("n", res.fullName());
    }
}

