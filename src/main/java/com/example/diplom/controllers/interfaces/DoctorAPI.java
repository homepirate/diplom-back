package com.example.diplom.controllers.interfaces;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.exceptions.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "doctors")
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
@RequestMapping("/api/doctors")
public interface DoctorAPI {

    @Operation(summary = "Получить все")
    @GetMapping(value = "/{page}")
    DoctorResponse getAllDoctors(@PathVariable("page") int page);

    @Operation(summary = "Получить даты и время всех визитов врача")
    @GetMapping(value = "/visits/dates")
    List<VisitDateResponse> getDoctorVisitDates();

    @Operation(summary = "Создать услугу для доктора")
    @PostMapping(value = "/services")
    ResponseEntity<?> createService(@RequestBody CreateServiceRequest serviceRequest);

    @Operation(summary = "Получить все услуги для доктора")
    @GetMapping(value = "/services")
    ResponseEntity<List<ServiceResponse>> getDoctorServices();


    @Operation(summary = "Создать визит для пациента")
    @PostMapping(value = "/visits")
    ResponseEntity<CreateVisitResponse> createVisit(@RequestBody CreateVisitRequest visitRequest);

    @Operation(summary = "Обновить цену услуги доктора")
    @PutMapping(value = "/services/update-price")
    ResponseEntity<StatusResponse> updateServicePrice(
            @RequestBody UpdateServiceRequest updateServiceRequest);

    @Operation(summary = "Получить всех пациентов доктора")
    @GetMapping(value = "/patients")
    ResponseEntity<List<PatientResponse>> getDoctorPatients();

    //сделать изменение только незавершенных визитов
    @Operation(summary = "Изменить дату и время визита")
    @PutMapping(value = "/visits/rearrange")
    ResponseEntity<StatusResponse> rearrangeVisit(@RequestBody RearrangeVisitRequest rearrangeRequest);

    //сделать изменение только незавершенных визитов
    @Operation(summary = "Отменить визит")
    @DeleteMapping(value = "/visits/cancel")
    ResponseEntity<StatusResponse> cancelVisit(@RequestParam("id") UUID id);

    @Operation(summary = "Получить описание визита")
    @GetMapping(value = "/visits/get-notes")
    ResponseEntity<VisitNotesResponse> getVisitNotes(@RequestParam("id") UUID id);

    @Operation(summary = "Завершить визит")
    @PutMapping(value = "/visits/finish")
    ResponseEntity<StatusResponse> finishVisit(@RequestBody FinishVisitRequest finishVisitRequest);


}