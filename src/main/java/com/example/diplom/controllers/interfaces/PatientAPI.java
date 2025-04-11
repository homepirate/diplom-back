package com.example.diplom.controllers.interfaces;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "patients")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успещная обработка запроса"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ресурс не найден", content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class)))
})
@RequestMapping("/api/patients")
public interface PatientAPI {

    @Operation(summary = "Получить все")
    @GetMapping(value = "/{page}")
    PatientResponse getAllPatients(@PathVariable("page") int page);

    @Operation(summary = "Добавить вложение в визит")
    @PostMapping(value = "/add-attachment", consumes = {"multipart/form-data"})
    ResponseEntity<?> AddAttachment(@ModelAttribute AddAttachmentRequest addAttachmentRequest);

    @Operation(summary = "удалить вложение")
    @DeleteMapping(value = "/delete-attachment")
    ResponseEntity<?> deleteAttachment(@RequestParam("url") String url);

    @Operation(summary = "Получить визиты пациента")
    @GetMapping(value = "/get-patient-visits")
    ResponseEntity<List<PatientVisitDetailsResponse>> getVisitsByPatient();

    @Operation(summary = "Получить всех врачей пациента")
    @GetMapping(value = "/doctors")
    ResponseEntity<List<DoctorResponse>> getPatientDoctors();

    @Operation(summary = "Получить профиль пациента")
    @GetMapping(value = "/profile")
    ResponseEntity<PatientProfileResponse> getPatientProfile();

    @Operation(summary = "Обновить профиль пациента")
    @PutMapping(value = "/profile")
    ResponseEntity<?> updatePatientProfile(@RequestBody @Valid UpdatePatientProfileRequest updateRequest);

    @Operation(summary = "Удалить (деперсонализировать) все данные пациента")
    @DeleteMapping("/delete-all-patient-data")
    ResponseEntity<StatusResponse> deleteAllPatientData();

    @Operation(summary = "Link patient with doctor by unique code")
    @PostMapping("/link-doctor")
    ResponseEntity<StatusResponse> linkDoctor(@RequestBody @Valid LinkDoctorRequest request);
}
