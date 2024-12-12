package com.example.diplom.repositories;

import com.example.diplom.models.PK.VisitServicePK;
import com.example.diplom.models.VisitService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitServiceRepository extends JpaRepository<VisitService, VisitServicePK> {
}