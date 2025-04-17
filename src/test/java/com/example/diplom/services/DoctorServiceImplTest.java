package com.example.diplom.services;

import com.example.diplom.controllers.RR.CreateServiceRequest;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Specialization;
import com.example.diplom.models.Visit;
import com.example.diplom.models.VisitService;
import com.example.diplom.repositories.*;
import com.example.diplom.services.dtos.VisitDto;
import com.example.diplom.services.implementations.DoctorServiceImpl;
import com.example.diplom.notif.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorServiceImplTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private SpecializationRepository specializationRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private VisitServiceRepository visitServiceRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModelMapper modelMapper;
    @Mock private NotificationMailService notificationService;
    @Mock private AttachmentService attachmentService;
    @Mock private DoctorPatientRepository doctorPatientRepository;

    @InjectMocks private DoctorServiceImpl service;

    private UUID doctorId;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doctorId = UUID.randomUUID();
        doctor = new Doctor();
        doctor.setId(doctorId);
    }

    @Test
    void createServiceForDoctor_whenNameExists_thenThrow() {
        // given
        var req = new CreateServiceRequest("X-ray", BigDecimal.valueOf(50));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByDoctorIdAndName(doctorId, "X-ray"))
                .thenReturn(Optional.of(new com.example.diplom.models.Service()));

        // when / then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createServiceForDoctor(doctorId, req)
        );
        assertEquals("Service with that name already exists.", ex.getMessage());
    }

    @Test
    void getDoctorVisitDates_whenDoctorNotFound_thenThrow() {
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getDoctorVisitDates(doctorId, 1, 2025)
        );
    }

    @Test
    void getDoctorVisitDates_returnsMappedDto() {
        // prepare visits
        Visit v = new Visit();
        v.setId(UUID.randomUUID());
        v.setFinished(true);
        v.setTotalCost(BigDecimal.valueOf(123));
        v.setNotes("note");
        List<Visit> visits = List.of(v);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(visitRepository.findByDoctorIdAndMonthYear(doctorId, 1, 2025))
                .thenReturn(visits);
        VisitDto dto = new VisitDto();
        when(modelMapper.map(v, VisitDto.class)).thenReturn(dto);

        // execute
        List<VisitDto> result = service.getDoctorVisitDates(doctorId, 1, 2025);

        // verify mapping
        assertSame(dto, result.get(0));
        assertTrue(dto.isFinished());
        assertEquals(BigDecimal.valueOf(123), dto.getTotalCost());
        assertEquals("note", dto.getNotes());
    }

    // ... you would add tests for registerDoctor, updateServicePrice,
    // finishVisit (including service‑update logic), createVisit overlap logic, etc.
}
