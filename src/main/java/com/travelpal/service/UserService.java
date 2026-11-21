package com.travelpal.service;

import com.travelpal.entity.User;
import com.travelpal.repository.UserRepository;
import com.travelpal.web.RegisterForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registers new users and loads users for Spring Security's login.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean usernameTaken(String username) {
        return userRepository.existsByUsername(username.trim());
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    /** All users, oldest account first (used by the admin page). */
    public List<User> findAllUsers() {
        return userRepository.findAll(Sort.by("createdAt"));
    }

    public User register(RegisterForm form) {
        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        return userRepository.save(user);
    }

    /** Login works with either the username or the email address. */
    @Override
    public UserDetails loadUserByUsername(String login) {
        User user = userRepository.findByUsername(login.trim())
                .or(() -> userRepository.findByEmail(login.trim().toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException(login));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
