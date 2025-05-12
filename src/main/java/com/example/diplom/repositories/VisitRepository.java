package com.example.diplom.repositories;

import com.example.diplom.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {

    @Query("SELECT v FROM Visit v WHERE v.doctor.id = :doctorId " +
            "AND EXTRACT(MONTH FROM v.visitDate) = :month " +
            "AND EXTRACT(YEAR FROM v.visitDate) = :year")
    List<Visit> findByDoctorIdAndMonthYear(UUID doctorId, int month, int year);

    @Query("SELECT v FROM Visit v WHERE v.doctor.id = :doctorId " +
            "AND CAST(v.visitDate AS DATE) = CAST(:date AS DATE)")
    List<Visit> findByDoctorIdAndDate(UUID doctorId, String date);

    List<Visit> findByDoctorIdAndVisitDateBetween(UUID doctorId, LocalDateTime start, LocalDateTime end);

    Optional<Visit> findByIdAndPatientId(UUID visitId, UUID patientId);

    @Query("SELECT v.notes FROM Visit v WHERE v.id = :id")
    String getNotesById(UUID id);


    List<Visit> findByPatientIdAndDoctorId(UUID patientId, UUID doctorId);

    List<Visit> findByPatientId(UUID patientId);

    @Query("SELECT v FROM Visit v WHERE v.visitDate BETWEEN :start AND :end AND v.finished = false")
    List<Visit> findVisitsByVisitDateBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
