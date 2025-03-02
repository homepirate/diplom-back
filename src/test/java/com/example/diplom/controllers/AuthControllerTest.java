package com.example.diplom.controllers;

import com.example.diplom.services.dtos.CustomUserDetails;
import com.example.diplom.utils.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // отключаем фильтры безопасности для тестов
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void testLoginSuccess() throws Exception {
        // Данные для теста
        String email = "user@example.com";
        String password = "password123";
        String token = "dummy-jwt-token";
        UUID userId = UUID.randomUUID();

        // Создаём кастомного пользователя (предполагается, что у CustomUserDetails есть соответствующий конструктор)
        CustomUserDetails customUserDetails = new CustomUserDetails(userId, email, password, "DOCTOR", "1111");

        // Мок authentication, возвращающий CustomUserDetails в качестве principal
        Authentication authenticationMock = Mockito.mock(Authentication.class);
        Mockito.when(authenticationMock.getPrincipal()).thenReturn(customUserDetails);
        Mockito.when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticationMock);
        Mockito.when(jwtTokenProvider.generateToken(eq(authenticationMock), eq(userId)))
                .thenReturn(token);

        // JSON-запрос
        String loginJson = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    public void testLoginFailure() throws Exception {
        String email = "user@example.com";
        String password = "wrongpassword";

        // Настраиваем мок, чтобы выбросить исключение при аутентификации
        Mockito.when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        String loginJson = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
