package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.*;
import com.example.diplom.controllers.interfaces.DoctorAPI;
import com.example.diplom.exceptions.StatusResponse;
import com.example.diplom.services.DoctorService;
import com.example.diplom.services.dtos.VisitDto;
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
    private DoctorService doctorService;

    @Autowired
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public DoctorResponse getAllDoctors(int page) {
        return null;
    }

    @Override
    public List<VisitDateResponse> getDoctorVisitDates() {
        List<VisitDto> visits = doctorService.getDoctorVisitDates(getDoctorId());

        return visits.stream()
                .map(visit -> new VisitDateResponse(
                        visit.getId(),
                        visit.getVisitDate(),
                        visit.getPatient() != null ? visit.getPatient().getFullName() : "Unknown",
                        visit.isFinished()
                ))
                .collect(Collectors.toList());
    }


    @Override
    public ResponseEntity<?> createService(CreateServiceRequest serviceRequest) {
        UUID doctorId = getDoctorId();
        doctorService.createServiceForDoctor(doctorId, serviceRequest);
        CreateServiceResponse createServiceResponse = new CreateServiceResponse("CREATED", "Service created");
        return ResponseEntity.status(HttpStatus.CREATED).body(createServiceResponse);

    }

    @Override
    public ResponseEntity<List<ServiceResponse>> getDoctorServices() {
        UUID doctorId = getDoctorId();
        List<ServiceResponse> services = doctorService.getDoctorServices(doctorId);
        return ResponseEntity.ok(services);
    }


    @Override
    public ResponseEntity<CreateVisitResponse> createVisit(@RequestBody CreateVisitRequest visitRequest) {
        UUID doctorId = getDoctorId();
        CreateVisitResponse response = doctorService.createVisit(doctorId, visitRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<StatusResponse> updateServicePrice(
            @RequestBody UpdateServiceRequest updateServiceRequest) {  // New price
        UUID doctorId = getDoctorId();
        doctorService.updateServicePrice(doctorId, updateServiceRequest);

        return ResponseEntity.ok(new StatusResponse("UPDATED", "Service price updated successfully"));
    }

    public ResponseEntity<List<PatientResponse>> getDoctorPatients() {
        UUID doctorId = getDoctorId();
        List<PatientResponse> patients = doctorService.getDoctorPatients(doctorId);
        return ResponseEntity.ok(patients);
    }

    UUID getDoctorId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID doctorId = UUID.fromString(jwt.getClaim("id"));
        return doctorId;
    }

    @Override
    public ResponseEntity<StatusResponse> rearrangeVisit(@RequestBody RearrangeVisitRequest rearrangeRequest) {
        UUID doctorId = getDoctorId();
        doctorService.rearrangeVisit(doctorId, rearrangeRequest);
        return ResponseEntity.ok(new StatusResponse("UPDATED", "Visit rearranged successfully"));
    }

    @Override
    public ResponseEntity<StatusResponse> cancelVisit(@RequestParam("id") UUID id) {
        doctorService.cancelVisit(new VisitIdRequest(id));
        return ResponseEntity.ok(new StatusResponse("UPDATED", "Visit cancelled"));
    }

    @Override
    public ResponseEntity<VisitNotesResponse> getVisitNotes(UUID id) {
        return ResponseEntity.ok(doctorService.getVisitDescription(new VisitIdRequest(id)));

    }

    @Override
    public ResponseEntity<StatusResponse> finishVisit(@RequestBody FinishVisitRequest finishVisitRequest) {
        doctorService.finishVisit(finishVisitRequest);
        return ResponseEntity.ok(new StatusResponse("UPDATE", "Visit finished"));
    }

    @Override
    public ResponseEntity<FinishVisitDataResponse> getFinishVisitData(@RequestParam("id") UUID id) {
        FinishVisitDataResponse response = doctorService.getFinishVisitData(new VisitIdRequest(id));
        return ResponseEntity.ok(response);
    }

}