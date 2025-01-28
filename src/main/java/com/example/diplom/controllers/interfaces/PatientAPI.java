package com.example.diplom.controllers.interfaces;

import com.example.diplom.controllers.RR.AddAttachmentRequest;
import com.example.diplom.controllers.RR.CreateVisitRequest;
import com.example.diplom.controllers.RR.CreateVisitResponse;
import com.example.diplom.controllers.RR.PatientResponse;
import com.example.diplom.exceptions.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "patients")
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
@RequestMapping("/api/patients")
public interface PatientAPI {

        @Operation(summary = "Получить все")
        @GetMapping(value = "/{page}")
        PatientResponse getAllPatients(@PathVariable("page") int page);

        @Operation(summary = "Добавить вложение в визит")
        @PostMapping(value = "/add-attachment", consumes = {"multipart/form-data"})
        ResponseEntity<?> AddAttachment(@ModelAttribute AddAttachmentRequest addAttachmentRequest);
}
