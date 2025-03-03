package com.example.diplom.controllers;

import com.example.diplom.repositories.SpecializationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecializationsController.class)
@AutoConfigureMockMvc(addFilters = false) // отключаем фильтры безопасности для тестов
public class SpecializationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecializationRepository specializationRepository;

    @Test
    public void testGetAllSpecializations() throws Exception {
        List<String> specializations = Arrays.asList("Cardiology", "Dermatology", "Neurology");
        when(specializationRepository.getAllNames()).thenReturn(specializations);

        mockMvc.perform(get("/api/specializations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(specializations.size())))
                .andExpect(jsonPath("$[0]", is("Cardiology")))
                .andExpect(jsonPath("$[1]", is("Dermatology")))
                .andExpect(jsonPath("$[2]", is("Neurology")));
    }
}
