package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.controllers.RR.DoctorResponse;
import com.example.diplom.controllers.RR.PatientProfileResponse;
import com.example.diplom.controllers.RR.PatientVisitDetailsResponse;
import com.example.diplom.exceptions.AlreadyLinkedException;
import com.example.diplom.exceptions.ResourceNotFoundException;
import com.example.diplom.services.AttachmentService;
import com.example.diplom.services.PatientService;
import com.example.diplom.services.dtos.AttachmentDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
public class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    @MockBean
    private PatientService patientService;

    // Фиксированный UUID для передачи в JWT claim "id"
    private final UUID patientId = UUID.randomUUID();

    // Для тестирования AddAttachment будем передавать валидные значения:
    // Файл и visitId (в формате UUID)
    private final MockMultipartFile dummyFile = new MockMultipartFile(
            "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "dummy content".getBytes());
    private final String validVisitId = "11111111-1111-1111-1111-111111111111";

    @Test
    public void testAddAttachmentSuccess() throws Exception {
        AttachmentDto dummyAttachment = new AttachmentDto();
        Mockito.when(attachmentService.addAttachment(eq(patientId), any(AddAttachmentRequest.class)))
                .thenReturn(dummyAttachment);

        mockMvc.perform(multipart("/api/patients/add-attachment")
                        .file(dummyFile)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("attachmentName", "testFile")
                        .param("description", "Test attachment")
                        .param("visitId", validVisitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Add"))
                .andExpect(jsonPath("$.message").value("Attachment added"));
    }

    @Test
    public void testAddAttachmentResourceNotFound() throws Exception {
        Mockito.when(attachmentService.addAttachment(eq(patientId), any(AddAttachmentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Resource not found"));

        mockMvc.perform(multipart("/api/patients/add-attachment")
                        .file(dummyFile)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("attachmentName", "testFile")
                        .param("visitId", validVisitId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    public void testAddAttachmentIOException() throws Exception {
        Mockito.when(attachmentService.addAttachment(eq(patientId), any(AddAttachmentRequest.class)))
                .thenThrow(new IOException("File error"));

        mockMvc.perform(multipart("/api/patients/add-attachment")
                        .file(dummyFile)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("attachmentName", "testFile")
                        .param("visitId", validVisitId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("File processing error"));
    }

    @Test
    public void testDeleteAttachmentSuccess() throws Exception {
        String url = "http://example.com/file.pdf";
        mockMvc.perform(delete("/api/patients/delete-attachment")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("url", url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Delete"))
                .andExpect(jsonPath("$.message").value("Attachment deleted successfully"));
    }

    @Test
    public void testDeleteAttachmentResourceNotFound() throws Exception {
        String url = "http://example.com/nonexistent.pdf";
        doThrow(new ResourceNotFoundException("Attachment not found"))
                .when(attachmentService).deleteAttachmentByUrl(eq(patientId), eq(url));

        mockMvc.perform(delete("/api/patients/delete-attachment")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("url", url))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("Attachment not found"));
    }

    @Test
    public void testDeleteAttachmentAccessDenied() throws Exception {
        String url = "http://example.com/file.pdf";
        doThrow(new AccessDeniedException("Access denied"))
                .when(attachmentService).deleteAttachmentByUrl(eq(patientId), eq(url));

        mockMvc.perform(delete("/api/patients/delete-attachment")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("url", url))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    public void testDeleteAttachmentGenericException() throws Exception {
        String url = "http://example.com/file.pdf";
        doThrow(new RuntimeException("Internal error"))
                .when(attachmentService).deleteAttachmentByUrl(eq(patientId), eq(url));

        mockMvc.perform(delete("/api/patients/delete-attachment")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .param("url", url))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    public void testGetPatientDoctors() throws Exception {
        List<DoctorResponse> doctors = Arrays.asList(
                new DoctorResponse("Doctor 1", "Cardiology", UUID.randomUUID()),
                new DoctorResponse("Doctor 2", "Dermatology", UUID.randomUUID())
        );
        Mockito.when(patientService.getPatientDoctors(eq(patientId))).thenReturn(doctors);

        mockMvc.perform(get("/api/patients/doctors")
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(doctors.size())))
                .andExpect(jsonPath("$[0].fullName").value("Doctor 1"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"))
                .andExpect(jsonPath("$[1].fullName").value("Doctor 2"))
                .andExpect(jsonPath("$[1].specialization").value("Dermatology"));
    }
    @Test
    public void testGetPatientProfileSuccess() throws Exception {
        PatientProfileResponse profile = new PatientProfileResponse(
                "Иван Иванов",
                LocalDate.of(1990, 5, 20),
                "ivan@mail.ru",
                "89001234567",
                List.of()  // пустой список вложений
        );

        Mockito.when(patientService.profileById(patientId)).thenReturn(profile);

        mockMvc.perform(get("/api/patients/profile")
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Иван Иванов"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"))
                .andExpect(jsonPath("$.phone").value("89001234567"))
                .andExpect(jsonPath("$.birthDate").value("1990-05-20"));
    }




    @Test
    public void testUpdatePatientProfileSuccess() throws Exception {
        String requestJson = """
        {
            "fullName": "Иван Иванов",
            "phone": "89001234567"
        }
    """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/patients/profile")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Update"))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));
    }

    @Test
    public void testDeleteAllPatientDataSuccess() throws Exception {
        // ✅ Мокаем вызов метода, чтобы избежать 500 ошибки при выполнении
        Mockito.doNothing().when(patientService).deleteAllPatientData(eq(patientId));

        mockMvc.perform(delete("/api/patients/delete-all-patient-data")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("delete"))
                .andExpect(jsonPath("$.message").value("Data deleted succesfully"));
    }


    @Test
    public void testLinkDoctorSuccess() throws Exception {
        String json = """
        {"doctorCode": "1234567"}
    """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/patients/link-doctor")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Link"))
                .andExpect(jsonPath("$.message").value("Patient linked with doctor successfully"));
    }

    @Test
    public void testLinkDoctorNotFound() throws Exception {
        String json = """
        {"doctorCode": "1234567"}
    """;

        doThrow(new ResourceNotFoundException("Doctor not found"))
                .when(patientService).linkPatientWithDoctor(eq(patientId), eq("1234567"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/patients/link-doctor")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("Doctor not found"));
    }

    @Test
    public void testLinkDoctorAlreadyLinked() throws Exception {
        String json = """
        {"doctorCode": "1234567"}
    """;

        doThrow(new AlreadyLinkedException("Already linked"))
                .when(patientService).linkPatientWithDoctor(eq(patientId), eq("1234567"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/patients/link-doctor")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("Error"))
                .andExpect(jsonPath("$.message").value("Already linked"));
    }


    @Test
    public void testGetVisitsByPatient() throws Exception {
        PatientVisitDetailsResponse visit1 = new PatientVisitDetailsResponse(
                "Dr. A",
                UUID.randomUUID(),
                LocalDateTime.of(2025, 3, 1, 10, 0),
                false,
                "Notes 1",
                new BigDecimal("100.00"),
                Collections.emptyList(),
                Collections.emptyList()
        );

        PatientVisitDetailsResponse visit2 = new PatientVisitDetailsResponse(
                "Dr. B",
                UUID.randomUUID(),
                LocalDateTime.of(2025, 3, 2, 12, 30),
                true,
                "Notes 2",
                new BigDecimal("200.00"),
                Collections.emptyList(),
                Collections.emptyList()
        );

        List<PatientVisitDetailsResponse> visits = Arrays.asList(visit1, visit2);
        Mockito.when(patientService.getVisitsByPatient(eq(patientId))).thenReturn(visits);

        mockMvc.perform(get("/api/patients/get-patient-visits")
                        .with(jwt().jwt(jwt -> jwt.claim("id", patientId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(visits.size())))
                .andExpect(jsonPath("$[0].doctorName").value("Dr. A"))
                .andExpect(jsonPath("$[0].visitId").value(visit1.visitId().toString()))
                .andExpect(jsonPath("$[0].visitDate").value("2025-03-01T10:00:00"))
                .andExpect(jsonPath("$[0].isFinished").value(false))
                .andExpect(jsonPath("$[0].notes").value("Notes 1"))
                .andExpect(jsonPath("$[0].totalPrice").value(100.00))
                .andExpect(jsonPath("$[1].doctorName").value("Dr. B"))
                .andExpect(jsonPath("$[1].visitId").value(visit2.visitId().toString()))
                .andExpect(jsonPath("$[1].visitDate").value("2025-03-02T12:30:00"))
                .andExpect(jsonPath("$[1].isFinished").value(true))
                .andExpect(jsonPath("$[1].notes").value("Notes 2"))
                .andExpect(jsonPath("$[1].totalPrice").value(200.00));
    }
}
