package com.example.diplom.controllers;

import com.example.diplom.models.Specialization;
import com.example.diplom.repositories.SpecializationRepository;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.PatientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false) // отключаем фильтры безопасности для тестов
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private PatientService patientService;

//    @Autowired
//    private SpecializationRepository specializationRepository;
//
//    public RegistrationControllerTest(SpecializationRepository specializationRepository) {
//        this.specializationRepository = specializationRepository;
//    }

    @Test
    public void testRegisterDoctor_Success() throws Exception {
        String doctorJson = """
                {
                    "email": "doctor@example.com",
                    "password": "secret123",
                    "phone": "89016314517",
                    "fullName": "Dr. John Doe",
                    "specialization": "Cardiology"
                }
                """;

        mockMvc.perform(post("/register/doctor")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("Doctor registered successfully!"));
    }

    @Test
    public void testRegisterDoctor_BadPhone() throws Exception {
        String doctorJson = """
                {
                    "email": "doctor@example.com",
                    "password": "secret123",
                    "phone": "1234567890",
                    "fullName": "Dr. John Doe",
                    "specialization": "Cardiology"
                }
                """;

        mockMvc.perform(post("/register/doctor")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Невалидный номер телефона. Длина 11 цифр и начинается с '8'."));
    }

@Test
    public void testRegisterDoctor_NoSpecialization() throws Exception {
    String doctorJson = """
            {
                "email": "doctor@example.com",
                "password": "secret123",
                "phone": "88005553535",
                "fullName": "Dr. John Doe",
                "specialization": ""
            }
            """;

    mockMvc.perform(post("/register/doctor")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(doctorJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Специализация не должна быть пустой"));

}


//    @Test
//    public void testRegisterDoctor_InvalidSpecialization() throws Exception {
//        String doctorJson = """
//                {
//                    "email": "doctor@example.com",
//                    "password": "secret123",
//                    "phone": "88005553535",
//                    "fullName": "Dr. John Doe",
//                    "specialization": "invalid-specialization"
//                }
//                """;
//
//        // Извлекаем значение специализации из JSON
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode root = mapper.readTree(doctorJson);
//        String specializationFromJson = root.get("specialization").asText();
//
//        // Проверяем, что специализация отсутствует в базе данных
//        Optional<Specialization> specializationOptional = specializationRepository.findByName(specializationFromJson);
//        assertTrue("Специализация должна отсутствовать в базе данных", specializationOptional.isEmpty());
//    }

    @Test
    public void testRegisterPatient_Success() throws Exception {
        String patientJson = """
                {
                    "email": "patient@example.com",
                    "password": "secret123",
                    "phone": "89035314517",
                    "fullName": "Jane Doe",
                    "birthDate": "1990-01-01"
                }
                """;

        mockMvc.perform(post("/register/patient")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.message").value("Patient registered successfully!"));
    }

    @Test
    public void testRegisterDoctor_InvalidEmail() throws Exception {
        String doctorJson = """
                {
                    "email": "invalid-email",
                    "password": "secret123",
                    "phone": "1234567890",
                    "fullName": "Dr. John Doe",
                    "specialization": "Cardiology"
                }
                """;

        mockMvc.perform(post("/register/doctor")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(doctorJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegisterPatient_InvalidBirthDate() throws Exception {
        String patientJson = """
                {
                    "email": "patient@example.com",
                    "password": "secret123",
                    "phone": "1234567890",
                    "fullName": "Jane Doe",
                    "birthDate": "2030-01-01"
                }
                """;

        mockMvc.perform(post("/register/patient")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson))
                .andExpect(status().isBadRequest());
    }
}
