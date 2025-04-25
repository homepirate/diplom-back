package com.example.diplom.services;


import com.example.diplom.models.Doctor;
import com.example.diplom.models.User;
import com.example.diplom.repositories.UserRepository;
import com.example.diplom.services.dtos.CustomUserDetails;
import com.example.diplom.services.implementations.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "secret";
    private final String ROLE = "ROLE_USER";

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void loadUserByUsername_userNotFound_throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername(EMAIL)
        );
        assertTrue(ex.getMessage().contains(EMAIL));
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void loadUserByUsername_doctor_returnsDetailsWithCodeAndName() {
        Doctor doc = new Doctor();
        doc.setId(userId);
        doc.setEmail(EMAIL);
        doc.setPassword(PASSWORD);
        doc.setRole("ROLE_DOCTOR");
        doc.setUniqueCode("DOC123");
        doc.setFullName("Dr. John Doe");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doc));

        UserDetails ud = service.loadUserByUsername(EMAIL);
        assertNotNull(ud);
        assertTrue(ud instanceof CustomUserDetails);
        CustomUserDetails cud = (CustomUserDetails) ud;

        assertEquals(userId, cud.getId());
        assertEquals(EMAIL, cud.getUsername());
        assertEquals(PASSWORD, cud.getPassword());

        assertEquals("DOC123", cud.getCode());
        assertEquals("Dr. John Doe", cud.getFullName());

        verify(userRepository).findByEmail(EMAIL);
    }
}
