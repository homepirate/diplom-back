package com.example.diplom.services.implementations;

import com.example.diplom.models.Doctor;
import com.example.diplom.models.User;
import com.example.diplom.repositories.UserRepository;
import com.example.diplom.services.dtos.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String code = null;
        String fullName = null;
        if (user instanceof Doctor) {
            code = ((Doctor) user).getUniqueCode();
            fullName = ((Doctor) user).getFullName();
        }

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                code,
                fullName
        );
    }
}
