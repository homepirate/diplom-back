package com.example.diplom.repositories;

import com.example.diplom.models.Service;
import com.example.diplom.models.Doctor;
import com.example.diplom.models.Specialization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class ServiceRepositoryTest {

    @Autowired ServiceRepository serviceRepository;
    @Autowired DoctorRepository doctorRepository;
    @Autowired SpecializationRepository specializationRepository;

    @Test
    void findByDoctorIdAndName_shouldReturnService() {
        // 1. Save specialization
        Specialization spec = new Specialization("Cardiology");
        specializationRepository.save(spec);

        // 2. Save doctor
        Doctor doc = new Doctor();
        doc.setEmail("doc@example.com");
        doc.setPassword("pass");
        doc.setRole("ROLE_DOCTOR");
        doc.setPhone("1234567890");
        doc.setFullName("Dr. John Doe");
        doc.setSpecialization(spec); // must be managed
        doc.setSpecializationName(spec.getName());
        doc.setUniqueCode("1234567");
        doctorRepository.save(doc);

        // 3. Save service
        Service svc = new Service();
        svc.setDoctor(doc);
        svc.setName("cleaning");
        svc.setPrice(BigDecimal.valueOf(100));
        serviceRepository.save(svc);

        // 4. ACT: find by doctor ID and name
        Optional<Service> found = serviceRepository.findByDoctorIdAndName(doc.getId(), "cleaning");

        // 5. ASSERT
        assertThat(found)
                .isPresent()
                .get()
                .extracting(Service::getPrice)
                .isEqualTo(BigDecimal.valueOf(100));
    }
}
