package com.example.diplom.repositories;

import com.example.diplom.models.PK.VisitServicePK;
import com.example.diplom.models.Visit;
import com.example.diplom.models.VisitService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitServiceRepository extends JpaRepository<VisitService, VisitServicePK> {

    List<VisitService> findByVisit(Visit visit);
}