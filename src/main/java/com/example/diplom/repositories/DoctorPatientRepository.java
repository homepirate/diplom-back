package com.example.diplom.repositories;

import com.example.diplom.models.Doctor;
import com.example.diplom.models.DoctorPatient;
import com.example.diplom.models.PK.DoctorPatientPK;
import com.example.diplom.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, DoctorPatientPK> {
    boolean existsById(DoctorPatientPK pk);
    @Query("SELECT dp.doctor FROM DoctorPatient dp WHERE dp.patient.id = :patientId")
    List<Doctor> findDoctorsByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT dp.patient FROM DoctorPatient dp WHERE dp.doctor.id = :doctorId")
    List<Patient> findPatientsByDoctorId(@Param("doctorId") UUID doctorId);
}