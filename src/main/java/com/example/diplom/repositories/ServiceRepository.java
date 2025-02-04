package com.example.diplom.repositories;

import com.example.diplom.models.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByNameIn(List<String> names);

    List<Service> findByDoctorId(UUID doctorId);

    Optional<Service> findByDoctorIdAndName(UUID doctorId, String name);

}
