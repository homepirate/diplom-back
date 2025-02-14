package com.example.diplom.repositories;

import com.example.diplom.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {
    List<Visit> findByDoctorId(UUID doctorId);

    Optional<Visit> findByIdAndPatientId(UUID visitId, UUID patientId);

    @Query("SELECT v.notes FROM Visit v WHERE v.id = :id")
    String getNotesById(UUID id);


    List<Visit> findByPatientIdAndDoctorId(UUID patientId, UUID doctorId);




}
