package com.example.diplom.repositories;

import com.example.diplom.models.DoctorPatient;
import com.example.diplom.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
}
