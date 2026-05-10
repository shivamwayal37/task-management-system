package com.portfolio.task_management_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.task_management_system.dto.AuthResponse;
import com.portfolio.task_management_system.dto.LoginRequest;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.repository.UserRepository;
import com.portfolio.task_management_system.utils.JwtUtil;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
            UserRepository userRepository, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user {}", request.getName());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getName(), request.getPassword()));

        log.info("Login successful for user {}", request.getName());
        User user = userRepository.findByName(request.getName());
        if (user != null) {
            auditService.logAction(user.getId(), "LOGIN", "USER", user.getId(), "User logged in");
        }
        return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(request.getName())));
    }
}
