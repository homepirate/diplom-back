package com.example.diplom.controllers;

import com.example.diplom.models.Specialization;
import com.example.diplom.repositories.SpecializationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/specializations")
public class SpecializationsController {
    private final SpecializationRepository specializationRepository;

    @Autowired
    public SpecializationsController(SpecializationRepository specializationRepository) {
        this.specializationRepository = specializationRepository;
    }

    @GetMapping()
    public List<String> getAllSpecializations() {
        return specializationRepository.getAllNames();
    }
}
