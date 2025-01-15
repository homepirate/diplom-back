package com.example.diplom.repositories;

import com.example.diplom.models.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
    Optional<Specialization> findByName(String name);
    @Query("SELECT s.name FROM Specialization s")
    List<String> getAllNames();
}
