package com.premtsd.linkedin.user.internal;

import com.premtsd.linkedin.user.JwtService;
import com.premtsd.linkedin.user.UserRegisteredEvent;

import com.premtsd.linkedin.user.internal.AuthDtos.LoginRequest;
import com.premtsd.linkedin.user.internal.AuthDtos.SignupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    @Transactional
    String signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("User already exists: " + req.email());
        }

        // SECURITY FIX vs the microservices version: roles are server-assigned,
        // never taken from the request body -> no privilege escalation at signup.
        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .roles(Set.of("USER"))
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}", saved.getId());

        // Published inside the tx; Spring Modulith persists it to the event log and
        // delivers it to async listeners after commit (durable, restart-safe).
        events.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getName(), saved.getEmail()));

        return jwtService.generate(saved.getId(), saved.getEmail(), saved.getRoles());
    }

    @Transactional(readOnly = true)
    String login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return jwtService.generate(user.getId(), user.getEmail(), user.getRoles());
    }
}
