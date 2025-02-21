package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.controllers.interfaces.DoctorAPI;
import com.example.diplom.exceptions.StatusResponse;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.VisitDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class DoctorController implements DoctorAPI {
    private static final Logger logger = LoggerFactory.getLogger(DoctorController.class);
    private DoctorService doctorService;

    @Autowired
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public DoctorResponse getAllDoctors(int page) {
        logger.debug("Вызов метода getAllDoctors с page: {}", page);
        return null;
    }

    @Override
    public List<VisitDateResponse> getDoctorVisitDates(int month, int year) {
        UUID doctorId = getDoctorId();
        logger.info("Получение дат посещений для доктора с id: {}, месяц: {}, год: {}", doctorId, month, year);
        List<VisitDto> visits = doctorService.getDoctorVisitDates(doctorId, month, year);

        List<VisitDateResponse> responses = visits.stream()
                .map(visit -> new VisitDateResponse(
                        visit.getId(),
                        visit.getVisitDate(),
                        visit.getPatient() != null ? visit.getPatient().getFullName() : "Unknown",
                        visit.getNotes(),
                        visit.isFinished()
                ))
                .collect(Collectors.toList());
        logger.debug("Найдено {} посещений", responses.size());
        return responses;
    }

    @Override
    public List<VisitDateResponse> getDoctorVisitDatesByDay(String date) {
        UUID doctorId = getDoctorId();
        logger.info("Получение дат посещений для доктора с id: {} на дату: {}", doctorId, date);
        List<VisitDto> visits = doctorService.getDoctorVisitDatesByDay(doctorId, date);

        List<VisitDateResponse> responses = visits.stream()
                .map(visit -> new VisitDateResponse(
                        visit.getId(),
                        visit.getVisitDate(),
                        visit.getPatient() != null ? visit.getPatient().getFullName() : "Unknown",
                        visit.getNotes(),
                        visit.isFinished()
                ))
                .collect(Collectors.toList());
        logger.debug("Найдено {} посещений на дату: {}", responses.size(), date);
        return responses;
    }


    @Override
    public ResponseEntity<?> createService(CreateServiceRequest serviceRequest) {
        UUID doctorId = getDoctorId();
        logger.info("Создание сервиса для доктора с id: {}. Данные запроса: {}", doctorId, serviceRequest);
        doctorService.createServiceForDoctor(doctorId, serviceRequest);
        CreateServiceResponse createServiceResponse = new CreateServiceResponse("CREATED", "Service created");
        logger.info("Сервис успешно создан для доктора с id: {}", doctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createServiceResponse);
    }

    @Override
    public ResponseEntity<List<ServiceResponse>> getDoctorServices() {
        UUID doctorId = getDoctorId();
        logger.info("Получение сервисов для доктора с id: {}", doctorId);
        List<ServiceResponse> services = doctorService.getDoctorServices(doctorId);
        logger.debug("Найдено {} сервисов для доктора с id: {}", services.size(), doctorId);
        return ResponseEntity.ok(services);
    }


    @Override
    public ResponseEntity<CreateVisitResponse> createVisit(@RequestBody CreateVisitRequest visitRequest) {
        UUID doctorId = getDoctorId();
        logger.info("Создание визита для доктора с id: {}. Запрос: {}", doctorId, visitRequest);
        CreateVisitResponse response = doctorService.createVisit(doctorId, visitRequest);
        logger.info("Визит успешно создан для доктора с id: {}", doctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<StatusResponse> updateServicePrice(@RequestBody UpdateServiceRequest updateServiceRequest) {
        UUID doctorId = getDoctorId();
        logger.info("Обновление цены сервиса для доктора с id: {}. Запрос: {}", doctorId, updateServiceRequest);
        doctorService.updateServicePrice(doctorId, updateServiceRequest);
        logger.info("Цена сервиса успешно обновлена для доктора с id: {}", doctorId);
        return ResponseEntity.ok(new StatusResponse("UPDATED", "Service price updated successfully"));
    }

    public ResponseEntity<List<PatientResponse>> getDoctorPatients() {
        UUID doctorId = getDoctorId();
        logger.info("Получение пациентов для доктора с id: {}", doctorId);
        List<PatientResponse> patients = doctorService.getDoctorPatients(doctorId);
        logger.debug("Найдено {} пациентов для доктора с id: {}", patients.size(), doctorId);
        return ResponseEntity.ok(patients);
    }


    @Override
    public ResponseEntity<StatusResponse> rearrangeVisit(@RequestBody RearrangeVisitRequest rearrangeRequest) {
        UUID doctorId = getDoctorId();
        logger.info("Перенос визита для доктора с id: {}. Запрос: {}", doctorId, rearrangeRequest);
        doctorService.rearrangeVisit(doctorId, rearrangeRequest);
        logger.info("Визит успешно перенесён для доктора с id: {}", doctorId);
        return ResponseEntity.ok(new StatusResponse("UPDATED", "Visit rearranged successfully"));
    }

    @Override
    public ResponseEntity<StatusResponse> cancelVisit(@RequestParam("id") UUID id) {
        UUID doctorId = getDoctorId();
        logger.info("Отмена визита с id: {} для доктора с id: {}", id, doctorId);
        doctorService.cancelVisit(doctorId, new VisitIdRequest(id));
        logger.info("Визит с id: {} успешно отменён для доктора с id: {}", id, doctorId);
        return ResponseEntity.ok(new StatusResponse("UPDATED", "Visit cancelled"));
    }


    @Override
    public ResponseEntity<StatusResponse> finishVisit(@RequestBody FinishVisitRequest finishVisitRequest) {
        UUID doctorId = getDoctorId();
        logger.info("Завершение визита для доктора с id: {}. Запрос: {}", doctorId, finishVisitRequest);
        doctorService.finishVisit(doctorId, finishVisitRequest);
        logger.info("Визит успешно завершён для доктора с id: {}", doctorId);
        return ResponseEntity.ok(new StatusResponse("UPDATE", "Visit finished"));
    }

    @Override
    public ResponseEntity<VisitDetailsResponse> getFinishVisitData(@RequestParam("id") UUID id) {
        UUID doctorId = getDoctorId();
        logger.info("Получение данных завершённого визита с id: {} для доктора с id: {}", id, doctorId);
        VisitDetailsResponse response = doctorService.getFinishVisitData(doctorId, new VisitIdRequest(id));
        logger.debug("Получены данные завершённого визита: {}", response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PatientMedCardResponse> getPatientMedicalCard(@RequestParam("patientId") UUID patientId) {
        UUID doctorId = getDoctorId();
        logger.info("Получение медицинской карты пациента с id: {} для доктора с id: {}", patientId, doctorId);
        PatientMedCardResponse response = doctorService.getPatientMedicalCard(doctorId, patientId);
        logger.debug("Получена медицинская карта для пациента с id: {}", patientId);
        return ResponseEntity.ok(response);
    }


    UUID getDoctorId() {
        logger.debug("Извлечение идентификатора доктора из JWT");
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID doctorId = UUID.fromString(jwt.getClaim("id"));
        logger.debug("Получен идентификатор доктора: {}", doctorId);
        return doctorId;
    }
}