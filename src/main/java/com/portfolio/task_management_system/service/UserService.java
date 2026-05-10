package com.portfolio.task_management_system.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.dto.CreateUserRequest;
import com.portfolio.task_management_system.dto.UserDTO;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.entity.UserRole;
import com.portfolio.task_management_system.exception.UserNotFoundException;
import com.portfolio.task_management_system.mapper.UserMapper;
import com.portfolio.task_management_system.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    public UserDTO createUser(CreateUserRequest request){
        log.info("Creating user with name {}", request.getName());
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        User savedUser = userRepository.save(user);
        log.info("Created user {}", savedUser.getId());
        return UserMapper.toDTO(savedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "users", key = "#id")
    public UserDTO getUserById(Long id) {
        log.info("Fetching user {}", id);
        log.info("Fetching tasks from DB");
        User user = getUserEntityById(id);
        return UserMapper.toDTO(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO getUser(String name){
        log.info("Fetching user by name {}", name);
        User user = userRepository.findByName(name);
        if (user == null) {
            throw new UserNotFoundException(name);
        }

        return UserMapper.toDTO(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "users", key = "#id")
    public UserDTO updateUser(Long id, CreateUserRequest request) {
        log.info("Updating user {}", id);
        User user = getUserEntityById(id);
        UserRole previousRole = user.getRole();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(toRole(request.getRole()));
        }

        User savedUser = userRepository.save(user);
        if (previousRole != savedUser.getRole()) {
            auditService.logAction(
                    "ROLE_CHANGE",
                    "USER",
                    savedUser.getId(),
                    "{\"before\":\"%s\",\"after\":\"%s\"}".formatted(previousRole, savedUser.getRole()));
        }
        log.info("Updated user {}", savedUser.getId());
        return UserMapper.toDTO(savedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id){
        log.info("Deleting user {}", id);
        User user = getUserEntityById(id);
        userRepository.delete(user);
        log.info("Deleted user {}", id);
    }

    private User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserRole toRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid user role: " + role);
        }
    }
}
