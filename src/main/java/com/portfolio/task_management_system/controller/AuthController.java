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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication APIs", description = "Authentication and JWT login operations")
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
    @Operation(summary = "Login and get JWT", description = "Authenticates a user and returns a JWT bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
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
