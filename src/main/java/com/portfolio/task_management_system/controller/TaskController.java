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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task Management APIs", description = "Manage tasks, assignments, status workflows and comments")
public class TaskController {
    
    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskCommentService taskCommentService;

    @GetMapping
    @Operation(summary = "List tasks", description = "Returns paginated tasks with an optional status filter.")
    public ResponseEntity<Page<TaskDTO>> getTasks(Pageable pageable,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(taskService.getTasks(pageable, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Fetches a task using its unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search task by title", description = "Fetches a task using an exact title search.")
    public ResponseEntity<TaskDTO> getTask(@RequestParam String title) {
        return ResponseEntity.ok(taskService.getTask(title));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates task title, description and status.")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status", description = "Applies a validated task status transition.")
    public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request.getStatus()));
    }

    @PatchMapping("/{taskId}/assign/{userId}")
    @Operation(summary = "Assign task", description = "Assigns a task to another user. Admin only.")
    public ResponseEntity<TaskDTO> assignTask(@PathVariable Long taskId, @PathVariable Long userId) {
        return ResponseEntity.ok(taskService.assignTask(taskId, userId));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add task comment", description = "Adds a comment to a task and emits a notification event.")
    public ResponseEntity<TaskCommentDTO> createComment(@PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(201).body(taskCommentService.createComment(id, request));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "List task comments", description = "Returns comments for a task in creation order.")
    public ResponseEntity<java.util.List<TaskCommentDTO>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(taskCommentService.getComments(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete task", description = "Marks a task as deleted without physically removing it.")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore task", description = "Restores a previously soft-deleted task.")
    public ResponseEntity<Void> restoreTask(@PathVariable Long id) {
        taskService.restoreTask(id);
        return ResponseEntity.noContent().build();
    }
}
