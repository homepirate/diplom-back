package com.example.diplom.services;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.AlreadyLinkedException;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.models.*;
import com.example.diplom.models.PK.DoctorPatientPK;
import com.example.diplom.repositories.*;
import com.example.diplom.services.dtos.PatientRegistrationDto;
import com.example.diplom.services.implementations.ChatServiceImpl;
import com.example.diplom.services.implementations.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModelMapper modelMapper;
    @Mock private VisitRepository visitRepository;
    @Mock private VisitServiceRepository visitServiceRepository;
    @Mock private AttachmentService attachmentService;
    @Mock private DoctorPatientRepository doctorPatientRepository;
    @Mock private ChatServiceImpl chatService;
    @Mock private DoctorRepository doctorRepository;

    @InjectMocks private PatientServiceImpl service;

    private UUID patientId;
    private Patient patient;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        patient = new Patient();
        patient.setId(patientId);
    }

    // ---------------- registerPatient ----------------

    @Test
    void registerPatient_newPatient_savesEncoded() {
        var req = new PatientRegisterRequest(
                "email@mail", "pw", "8000", "Name", LocalDate.of(2000,1,1));

        when(patientRepository.findByPhone("8000")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        doAnswer(invocation -> {
            PatientRegistrationDto dto = invocation.getArgument(0);
            assertEquals("encoded", dto.getPassword());
            return patient;
        }).when(modelMapper).map(any(PatientRegistrationDto.class), eq(Patient.class));

        service.registerPatient(req);

        verify(patientRepository).save(patient);
    }

    @Test
    void registerPatient_existingTemporary_migrates() {
        patient.setIsTemporary(true);
        when(patientRepository.findByPhone("8000")).thenReturn(Optional.of(patient));
        when(passwordEncoder.encode("pw")).thenReturn("enc");

        var req = new PatientRegisterRequest(
                "new@mail", "pw", "8000", "N", LocalDate.of(1990,1,1));

        service.registerPatient(req);

        assertFalse(patient.getIsTemporary());
        assertEquals("new@mail", patient.getEmail());
        assertEquals("N", patient.getFullName());
        verify(patientRepository).save(patient);
    }

    @Test
    void registerPatient_existingPermanent_throws() {
        patient.setIsTemporary(false);
        when(patientRepository.findByPhone("8000")).thenReturn(Optional.of(patient));

        var req = new PatientRegisterRequest(
                "x@y", "p", "8000", "N", LocalDate.now());

        assertThrows(IllegalArgumentException.class,
                () -> service.registerPatient(req));
        verify(patientRepository, never()).save(any());
    }

    // ---------------- getVisitsByPatient ----------------

    @Test
    void getVisitsByPatient_notFound_throws() {
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getVisitsByPatient(patientId));
    }

    @Test
    void getVisitsByPatient_returnsMappedList() throws Exception {
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        Doctor doc = new Doctor();
        doc.setFullName("Doc");
        visit.setDoctor(doc);
        visit.setVisitDate(LocalDateTime.of(2025,4,1,12,0));
        visit.setFinished(true);
        visit.setNotes("note");
        visit.setTotalCost(java.math.BigDecimal.TEN);

        Attachment att = new Attachment();
        att.setId(UUID.randomUUID());
        visit.setAttachments(new HashSet<>(List.of(att)));

        when(visitRepository.findByPatientId(patientId))
                .thenReturn(List.of(visit));
        when(visitServiceRepository.findByVisit(visit))
                .thenReturn(Collections.emptyList());
        when(attachmentService.getPresignedUrlForAttachment(att.getId()))
                .thenReturn("url");

        var respList = service.getVisitsByPatient(patientId);

        assertEquals(1, respList.size());
        var resp = respList.get(0);
        assertEquals("Doc", resp.doctorName());
        assertEquals(visit.getId(), resp.visitId());
        assertTrue(resp.isFinished());
        assertEquals("note", resp.notes());
        assertEquals(java.math.BigDecimal.TEN, resp.totalPrice());
        assertEquals(List.of("url"), resp.attachmentUrls());
    }

    // ---------------- getPatientDoctors ----------------

    @Test
    void getPatientDoctors_returnsList() {
        Doctor d = new Doctor();
        d.setId(UUID.randomUUID());
        d.setFullName("Dr X");
        Specialization spec = new Specialization("Spec");
        d.setSpecialization(spec);

        when(doctorPatientRepository.findDoctorsByPatientId(patientId))
                .thenReturn(List.of(d));

        var list = service.getPatientDoctors(patientId);
        assertEquals(1, list.size());
        assertEquals("Dr X", list.get(0).fullName());
        assertEquals("Spec", list.get(0).specialization());
    }

    // ---------------- profileById ----------------

    @Test
    void profileById_notFound_throws() {
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.profileById(patientId));
    }

    @Test
    void profileById_returnsProfile() throws Exception {
        patient.setFullName("N");
        patient.setBirthDate(LocalDate.of(1995,5,5));
        patient.setEmail("a@b");
        patient.setPhone("9000");

        Attachment att = new Attachment();
        att.setId(UUID.randomUUID());
        Visit v = new Visit();
        v.setAttachments(new HashSet<>(List.of(att)));
        patient.setVisits(new HashSet<>(List.of(v)));

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(attachmentService.getPresignedUrlForAttachment(att.getId()))
                .thenReturn("u");

        var resp = service.profileById(patientId);
        assertEquals("N", resp.fullName());
        assertEquals(LocalDate.of(1995,5,5), resp.birthDate());
        assertEquals("a@b", resp.email());
        assertEquals("9000", resp.phone());
        assertEquals(List.of("u"), resp.attachments());
    }

    // ---------------- deleteAllPatientData ----------------

    @Test
    void deleteAllPatientData_deletesAndDepersonalizes() throws Exception {
        Attachment att = new Attachment();
        att.setId(UUID.randomUUID());
        Visit v = new Visit();
        v.setAttachments(new HashSet<>(List.of(att)));
        patient.setVisits(new HashSet<>(List.of(v)));

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        service.deleteAllPatientData(patientId);

        verify(attachmentService).deleteAttachmentById(att.getId());
        verify(visitRepository).save(v);
        verify(patientRepository).save(patient);
        verify(chatService).deleteAllMessagesForUser(patientId.toString());
        assertTrue(patient.getFullName().startsWith("удален"));
    }

    // ---------------- updatePatientProfile ----------------

    @Test
    void updatePatientProfile_updatesFields() {
        patient.setFullName("old");
        patient.setEmail("e");
        patient.setPhone("p");
        patient.setBirthDate(LocalDate.of(2000,1,1));
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        var req = new UpdatePatientProfileRequest(
                "New", LocalDate.of(2001,2,2), "new@mail", "111");

        service.updatePatientProfile(patientId, req);

        assertEquals("New", patient.getFullName());
        assertEquals("new@mail", patient.getEmail());
        assertEquals("111", patient.getPhone());
        assertEquals(LocalDate.of(2001,2,2), patient.getBirthDate());
        verify(patientRepository).save(patient);
    }

    // ---------------- linkPatientWithDoctor ----------------

    @Test
    void linkPatientWithDoctor_success() {
        Doctor doc = new Doctor();
        doc.setId(UUID.randomUUID());
        when(doctorRepository.findByUniqueCode("code"))
                .thenReturn(Optional.of(doc));
        when(doctorPatientRepository.existsById(any()))
                .thenReturn(false);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        service.linkPatientWithDoctor(patientId, "code");

        ArgumentCaptor<DoctorPatient> capt =
                ArgumentCaptor.forClass(DoctorPatient.class);
        verify(doctorPatientRepository).save(capt.capture());
        assertEquals(doc, capt.getValue().getDoctor());
        assertEquals(patient, capt.getValue().getPatient());
    }

    @Test
    void linkPatientWithDoctor_doctorNotFound_throws() {
        when(doctorRepository.findByUniqueCode("code"))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.linkPatientWithDoctor(patientId, "code"));
    }

    @Test
    void linkPatientWithDoctor_alreadyLinked_throws() {
        Doctor doc = new Doctor();
        doc.setId(UUID.randomUUID());
        when(doctorRepository.findByUniqueCode("code"))
                .thenReturn(Optional.of(doc));
        when(doctorPatientRepository.existsById(
                new DoctorPatientPK(doc.getId(), patientId)))
                .thenReturn(true);

        assertThrows(AlreadyLinkedException.class,
                () -> service.linkPatientWithDoctor(patientId, "code"));
    }

    @Test
    void linkPatientWithDoctor_patientNotFound_throws() {
        Doctor doc = new Doctor();
        doc.setId(UUID.randomUUID());
        when(doctorRepository.findByUniqueCode("code"))
                .thenReturn(Optional.of(doc));
        when(doctorPatientRepository.existsById(any()))
                .thenReturn(false);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.linkPatientWithDoctor(patientId, "code"));
    }
}
