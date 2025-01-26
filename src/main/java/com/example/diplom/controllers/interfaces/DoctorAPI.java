package com.example.diplom.controllers.interfaces;

import com.example.diplom.controllers.RR.CreateServiceRequest;
import com.example.diplom.controllers.RR.DoctorResponse;
import com.example.diplom.controllers.RR.PatientResponse;
import com.example.diplom.controllers.RR.VisitDateResponse;
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
        @ApiResponse(responseCode = "400", description = "Ошибка валидации",  content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ресурс не найден",  content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",  content =
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

}