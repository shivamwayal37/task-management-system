package com.portfolio.task_management_system.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.service.TaskService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TaskService taskService;

    public AdminController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks/deleted")
    public ResponseEntity<Page<TaskDTO>> getDeletedTasks(Pageable pageable) {
        return ResponseEntity.ok(taskService.getDeletedTasks(pageable));
    }

    @DeleteMapping("/tasks/{id}/hard-delete")
    public ResponseEntity<Void> hardDeleteTask(@PathVariable Long id) {
        taskService.hardDeleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
