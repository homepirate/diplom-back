package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.AuthResponse;
import com.example.diplom.controllers.RR.LoginRequest;
import com.example.diplom.services.dtos.CustomUserDetails;
import com.example.diplom.utils.JwtTokenProvider;
import com.example.diplom.exceptions.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Получен запрос на логин для email: {}", loginRequest.email());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("Пользователь успешно аутентифицирован: {}", loginRequest.email());


            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = getUserIdFromUserDetails(userDetails);

            logger.debug("Извлечён userId");

            String token = jwtTokenProvider.generateToken(authentication, userId);
            logger.info("JWT токен сгенерирован для userId");


            AuthResponse authResponse = new AuthResponse(
                    "SUCCESS",
                    "Authentication successful",
                    token
            );

            return ResponseEntity.status(HttpStatus.OK).body(authResponse);
        } catch (Exception e) {
            logger.error("Ошибка аутентификации для email: {}. Сообщение: {}", loginRequest.email(), e.getMessage(), e);
            StatusResponse errorResponse = new StatusResponse(
                    "UNAUTHORIZED",
                    "Invalid credentials"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    private UUID getUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails) {
            UUID id = ((CustomUserDetails) userDetails).getId();
            logger.debug("Получен userId из CustomUserDetails");
            return id;
        }
        logger.error("UserDetails не содержит user ID, получен класс: {}", userDetails.getClass());
        throw new IllegalStateException("UserDetails does not contain user ID");
    }
}
