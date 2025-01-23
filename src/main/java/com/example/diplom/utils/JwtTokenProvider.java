package com.example.diplom.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private SecretKey getSigningKey() {
        String jwtSecret = "fad397b68badbbd0917c10f2557a1d2ccc81fab1ddd58cb3c820ee55c5aa6978";
        byte[] decodedKey = jwtSecret.getBytes();
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
    }

    public String generateToken(Authentication authentication, UUID userId) {
        String email = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Returns "ROLE_DOCTOR"
                .map(role -> role.startsWith("ROLE_")
                        ? role.substring(5) // Remove "ROLE_"
                        : role)
                .toList();

        int jwtExpirationMs = 86400000; // 1 day expiration

        return Jwts.builder()
                .claim("roles", roles)
                .claim("id", userId) // Add user ID
                .claim("email", email) // Use email instead of subject
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
