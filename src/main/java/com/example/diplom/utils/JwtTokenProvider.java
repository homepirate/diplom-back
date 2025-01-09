package com.example.diplom.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    public String generateToken(Authentication authentication) {
        String username = authentication.getName(); // email
        String jwtSecret = "fad397b68badbbd0917c10f2557a1d2ccc81fab1ddd58cb3c820ee55c5aa6978"; // 24 hours
        int jwtExpirationMs = 86400000;
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }
}

