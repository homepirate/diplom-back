package com.example.diplom.controllers;

import com.example.diplom.controllers.RR.AuthResponse;
import com.example.diplom.controllers.RR.LoginRequest;
import com.example.diplom.services.dtos.CustomUserDetails;
import com.example.diplom.utils.JwtTokenProvider;
import com.example.diplom.exceptions.StatusResponse;
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

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("Login request received for: " + loginRequest.email());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);


            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UUID userId = getUserIdFromUserDetails(userDetails);

            String token = jwtTokenProvider.generateToken(authentication, userId);

            AuthResponse authResponse = new AuthResponse(
                    "SUCCESS",
                    "Authentication successful",
                    token
            );

            return ResponseEntity.status(HttpStatus.OK).body(authResponse);
        } catch (Exception e) {
            StatusResponse errorResponse = new StatusResponse(
                    "UNAUTHORIZED",
                    "Invalid credentials"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    private UUID getUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails) {
            return ((CustomUserDetails) userDetails).getId();
        }
        throw new IllegalStateException("UserDetails does not contain user ID");
    }
}
