package com.example.diplom.controllers;

import com.example.diplom.models.Doctor;
import com.example.diplom.repositories.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DoctorControllerIT {

    @Autowired private MockMvc mvc;
    @Autowired private DoctorRepository doctorRepository;

    private UUID doctorId;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        Doctor d = new Doctor();
        d.setEmail("doc@test");
        d.setPassword("pass");
        d.setRole("ROLE_DOCTOR");
        doctorId = doctorRepository.save(d).getId();
    }

    @Test
    void createService_requiresAuth() throws Exception {
        // without JWT → 401
        mvc.perform(
                        post("/api/doctors/services")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"x-ray\",\"price\":50}")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createService_withValidJwtAndBody_creates() throws Exception {
        // build a JWT containing the claim "id" = doctorId
        var jwtPost = jwt()
                .jwt(jwt -> jwt.claim("id", doctorId.toString()))
                .authorities(() -> List.of().toString());

        mvc.perform(
                        post("/api/doctors/services")
                                .with(jwtPost)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"ultrasound\",\"price\":75}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("Service created"));
    }
    @Test
    void createService_withWrongDoctorId_forbidden() throws Exception {
        var badJwt = jwt()
                .jwt(jwt -> jwt.claim("id", UUID.randomUUID().toString()))
                .authorities(() -> String.valueOf(List.of("ROLE_DOCTOR")));

        mvc.perform(
                        post("/api/doctors/services")
                                .with(badJwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"ultrasound\",\"price\":75}")
                )
                .andExpect(status().isForbidden());
    }

}
