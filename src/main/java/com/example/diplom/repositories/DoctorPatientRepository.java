package com.example.diplom.repositories;

import com.example.diplom.models.DoctorPatient;
import com.example.diplom.models.PK.DoctorPatientPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorPatientRepository extends JpaRepository<DoctorPatient, DoctorPatientPK> {
    boolean existsById(DoctorPatientPK pk);

}