package com.example.diplom.security;

import com.example.diplom.models.PK.DoctorPatientPK;
import com.example.diplom.models.Service;
import com.example.diplom.models.Visit;
import com.example.diplom.repositories.DoctorPatientRepository;
import com.example.diplom.repositories.ServiceRepository;
import com.example.diplom.repositories.VisitRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * This component holds all the logic to verify that
 * the doctor in the JWT actually owns or is associated
 * with the resource being accessed.
 */
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

    /**
     * Utility: Extract the UUID from the JWT claim "id".
     */
    private UUID getDoctorIdFromAuth(Authentication authentication) {
        if (authentication == null) return null;
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("id"));
    }

    /**
     * 1) Check that the #doctorId in the method argument
     *    actually matches the ID in the JWT.
     */
    public boolean matchDoctorId(Authentication authentication, UUID doctorId) {
        UUID actualDoctorId = getDoctorIdFromAuth(authentication);
        if (actualDoctorId == null) return false;
        return actualDoctorId.equals(doctorId);
    }

    /**
     * 2) Check that the service belongs to the doctor indicated by #doctorId.
     *    We also verify the caller’s JWT matches #doctorId.
     */
    public boolean hasDoctorServiceOwnership(Authentication authentication, UUID doctorId, String serviceName) {
        // First ensure the JWT’s id matches #doctorId
        if (!matchDoctorId(authentication, doctorId)) {
            return false;
        }
        // Next ensure service is indeed owned by that doctor
        return serviceRepository.findByDoctorIdAndName(doctorId, serviceName).isPresent();
    }

    /**
     * 3) Check that the doc–patient link exists in the DB, and
     *    the JWT’s doc ID matches #doctorId.
     */
    public boolean hasDoctorPatientOwnership(Authentication authentication, UUID doctorId, UUID patientId) {
        if (!matchDoctorId(authentication, doctorId)) {
            return false;
        }
        return doctorPatientRepository.existsById(new DoctorPatientPK(doctorId, patientId));
    }

    /**
     * 4) Check that the visit belongs to #doctorId,
     *    and the JWT’s doc ID matches #doctorId.
     */
    public boolean hasDoctorVisitOwnership(Authentication authentication, UUID doctorId, UUID visitId) {
        if (!matchDoctorId(authentication, doctorId)) {
            return false;
        }
        Visit visit = visitRepository.findById(visitId).orElse(null);
        if (visit == null) return false;
        return visit.getDoctor().getId().equals(doctorId);
    }
}
