package com.example.diplom.controllers;

import com.example.diplom.models.Specialization;
import com.example.diplom.repositories.SpecializationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specializations")
public class SpecializationsController {
    private static final Logger logger = LoggerFactory.getLogger(SpecializationsController.class);

    private final SpecializationRepository specializationRepository;

    @Autowired
    public SpecializationsController(SpecializationRepository specializationRepository) {
        this.specializationRepository = specializationRepository;
    }

    @GetMapping()
    public List<String> getAllSpecializations() {
        logger.info("Запрос списка специализаций");
        List<String> specializations = specializationRepository.getAllNames();
        logger.debug("Найдено специализаций: {}", specializations.size());
        return specializations;
    }
}
