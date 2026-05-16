package com.portfolio.task_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.task_management_system.dto.CreateCommentRequest;
import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.TaskCommentDTO;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.dto.UpdateTaskStatusRequest;
import com.portfolio.task_management_system.service.TaskCommentService;
import com.portfolio.task_management_system.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskCommentService taskCommentService;

    @GetMapping
    public ResponseEntity<Page<TaskDTO>> getTasks(Pageable pageable,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(taskService.getTasks(pageable, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<TaskDTO> getTask(@RequestParam String title) {
        return ResponseEntity.ok(taskService.getTask(title));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request.getStatus()));
    }

    @PatchMapping("/{taskId}/assign/{userId}")
    public ResponseEntity<TaskDTO> assignTask(@PathVariable Long taskId, @PathVariable Long userId) {
        return ResponseEntity.ok(taskService.assignTask(taskId, userId));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskCommentDTO> createComment(@PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(201).body(taskCommentService.createComment(id, request));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<java.util.List<TaskCommentDTO>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(taskCommentService.getComments(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restoreTask(@PathVariable Long id) {
        taskService.restoreTask(id);
        return ResponseEntity.noContent().build();
    }
}
