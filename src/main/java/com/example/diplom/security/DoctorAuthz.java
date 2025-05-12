package com.example.diplom.security;

import com.example.diplom.models.PK.DoctorPatientPK;
import com.example.diplom.models.Visit;
import com.example.diplom.repositories.DoctorPatientRepository;
import com.example.diplom.repositories.ServiceRepository;
import com.example.diplom.repositories.VisitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component("doctorAuthz")
public class DoctorAuthz {

    private final DoctorPatientRepository doctorPatientRepository;
    private final ServiceRepository serviceRepository;
    private final VisitRepository visitRepository;

    public DoctorAuthz(DoctorPatientRepository doctorPatientRepository,
                       ServiceRepository serviceRepository,
                       VisitRepository visitRepository) {
        this.doctorPatientRepository = doctorPatientRepository;
        this.serviceRepository = serviceRepository;
        this.visitRepository = visitRepository;
    }

    private UUID getDoctorIdFromAuth(Authentication authentication) {
        if (authentication == null) return null;
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("id"));
    }

    public boolean matchDoctorId(Authentication authentication, UUID doctorId) {
        UUID actualDoctorId = getDoctorIdFromAuth(authentication);
        if (actualDoctorId == null) return false;
        return actualDoctorId.equals(doctorId);
    }

    public boolean hasDoctorServiceOwnership(Authentication authentication, UUID doctorId, String serviceName) {
        if (!matchDoctorId(authentication, doctorId)) {
            return false;
        }
        return serviceRepository.findByDoctorIdAndName(doctorId, serviceName).isPresent();
    }

    public boolean hasDoctorPatientOwnership(Authentication authentication, UUID doctorId, UUID patientId) {
        if (!matchDoctorId(authentication, doctorId)) {
            return false;
        }
        return doctorPatientRepository.existsById(new DoctorPatientPK(doctorId, patientId));
    }

    public boolean hasDoctorVisitOwnership(Authentication authentication, UUID doctorId, UUID visitId) {
        if (!matchDoctorId(authentication, doctorId)) {
            return false;
        }
        Visit visit = visitRepository.findById(visitId).orElse(null);
        if (visit == null) return false;
        return visit.getDoctor().getId().equals(doctorId);
    }
}
