package com.portfolio.task_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.CreateUserRequest;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.dto.UserDTO;
import com.portfolio.task_management_system.service.TaskService;
import com.portfolio.task_management_system.service.UserService;
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
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/profile/{name}")
    public ResponseEntity<UserDTO> getUserByName(@PathVariable String name) {
        return ResponseEntity.ok(userService.getUser(name));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PostMapping("/{userId}/tasks")
    public ResponseEntity<TaskDTO> createTaskForUser(@PathVariable Long userId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(userId, request));
    }

    @GetMapping("/{userId}/tasks")
    public ResponseEntity<Page<TaskDTO>> getTasksByUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksByUserId(userId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
