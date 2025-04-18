package com.example.diplom.controllers;

import com.example.diplom.models.*;
import com.example.diplom.repositories.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DoctorControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private VisitRepository visitRepository;             // ← new
    @Autowired private ServiceRepository serviceRepository;         // ← if you have one
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private SpecializationRepository specializationRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired DoctorPatientRepository doctorPatientRepository;  // ← add this


    private UUID doctorId;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAll();
        serviceRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        specializationRepository.deleteAll();

        var spec = new Specialization("Spec");
        specializationRepository.save(spec);

        doctor = new Doctor();
        doctor.setFullName("Dr. Test");
        doctor.setEmail("doc@test");
        doctor.setPhone("87777777777");
        doctor.setPassword("pass");
        doctor.setRole("ROLE_DOCTOR");
        doctor.setSpecialization(spec);
        doctor.setUniqueCode("UC");
        doctorId = doctorRepository.save(doctor).getId();
    }


    @Test
    void createService_requiresAuth() throws Exception {
        mvc.perform(post("/api/doctors/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x-ray\",\"price\":50}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createService_withValidJwt_creates() throws Exception {
        var jwtPost = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                .jwt(j -> j.claim("id", doctorId.toString()));
        mvc.perform(post("/api/doctors/services")
                        .with(jwtPost)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ultrasound\",\"price\":75}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getDoctorServices_requiresAuth() throws Exception {
        mvc.perform(get("/api/doctors/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDoctorServices_withValidJwt_returnsEmptyList() throws Exception {
        var jwtPost = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                .jwt(j -> j.claim("id", doctorId.toString()));
        mvc.perform(get("/api/doctors/services")
                        .with(jwtPost))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createVisit_requiresAuth() throws Exception {
        mvc.perform(post("/api/doctors/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createVisit_withValidJwt_createsVisit() throws Exception {
        // 1) create & save the patient with all required fields
        Patient p = new Patient();
        p.setFullName("PN");
        p.setEmail("p@test.com");
        p.setPhone("89000000000");
        p.setPassword("pass");
        p.setRole("ROLE_PATIENT");
        p.setBirthDate(LocalDate.now());
        patientRepository.save(p);

        // 2) link them so PreAuthorize passes
        var link = new DoctorPatient();
        link.setDoctor(doctor);
        link.setPatient(p);
        doctorPatientRepository.save(link);

        // 3) now call the API
        String body = String.format(
                "{\"patientId\":\"%s\",\"visitDate\":\"%s\",\"notes\":\"n\",\"force\":false}",
                p.getId(), LocalDateTime.now().toString()
        );

        var jwt = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))
                .jwt(j -> j.claim("id", doctorId.toString()));

        mvc.perform(post("/api/doctors/visits")
                        .with(jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitDate").exists());
    }
}

