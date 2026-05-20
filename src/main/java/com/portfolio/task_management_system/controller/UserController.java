package com.portfolio.task_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.CreateUserRequest;
import com.portfolio.task_management_system.dto.NotificationPreferenceDTO;
import com.portfolio.task_management_system.dto.NotificationPreferenceRequest;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.dto.UserDTO;
import com.portfolio.task_management_system.service.TaskService;
import com.portfolio.task_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management APIs", description = "Manage users, profiles and notification preferences")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @GetMapping
    @Operation(summary = "List users", description = "Returns all users. Admin only.")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the authenticated user from the JWT context.")
    public ResponseEntity<UserDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/preferences/notifications")
    @Operation(summary = "Update notification preference", description = "Updates the authenticated user's digest preference.")
    public ResponseEntity<NotificationPreferenceDTO> updateNotificationPreference(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUserNotificationPreference(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a user by id. Admin only.")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/profile/{name}")
    @Operation(summary = "Get user by name", description = "Returns a user by username. Admin only.")
    public ResponseEntity<UserDTO> getUserByName(@PathVariable String name) {
        return ResponseEntity.ok(userService.getUser(name));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Registers a new user account.")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PostMapping("/{userId}/tasks")
    @Operation(summary = "Create task for user", description = "Creates a task assigned to a specific user.")
    public ResponseEntity<TaskDTO> createTaskForUser(@PathVariable Long userId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(userId, request));
    }

    @GetMapping("/{userId}/tasks")
    @Operation(summary = "List user tasks", description = "Returns paginated tasks assigned to a user.")
    public ResponseEntity<Page<TaskDTO>> getTasksByUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksByUserId(userId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates user account details. Admin only.")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user account. Admin only.")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
