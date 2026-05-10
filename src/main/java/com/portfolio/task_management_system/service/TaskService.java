package com.portfolio.task_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskStatus;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.exception.TaskNotFoundException;
import com.portfolio.task_management_system.exception.UserNotFoundException;
import com.portfolio.task_management_system.mapper.TaskMapper;
import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentUser(#userId, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskDTO createTask(Long userId, CreateTaskRequest request){
        log.info("Creating task for user {}", userId);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            Task task = TaskMapper.toEntity(request);
            task.setUser(user);
            Task savedTask = taskRepository.save(task);
            auditService.logAction(
                    "CREATE_TASK",
                    "TASK",
                    savedTask.getId(),
                    "{\"title\":\"%s\",\"status\":\"%s\",\"ownerUserId\":%d}"
                            .formatted(escapeJson(savedTask.getTitle()), savedTask.getStatus(), user.getId()));
            log.info("Created task {} for user {}", savedTask.getId(), userId);
            return TaskMapper.toDTO(savedTask);
        } catch (RuntimeException ex) {
            log.error("Task creation failed for user {}", userId, ex);
            throw ex;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(
            value = "tasks",
            key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString() + '-' + (#status == null ? 'ALL' : #status)")
    public Page<TaskDTO> getTasks(Pageable pageable, String status) {
        log.info("Fetching tasks page={} size={} sort={} status={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort(),
                status);

        if (status == null || status.isBlank()) {
            return taskRepository.findAll(pageable).map(TaskMapper::toDTO);
        }

        return taskRepository.findByStatus(toStatus(status), pageable).map(TaskMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentUser(#userId, authentication.name)")
    public Page<TaskDTO> getTasksByUserId(Long userId, Pageable pageable) {
        log.info("Fetching tasks for user {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return taskRepository.findByUserId(userId, pageable)
                .map(TaskMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    public TaskDTO getTaskById(Long id) {
        log.info("Fetching task {}", id);
        Task task = getTaskEntityById(id);
        return TaskMapper.toDTO(task);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public TaskDTO getTask(String title){
        log.info("Searching task by title {}", title);
        Task task = taskRepository.findByTitle(title);
        if (task == null) {
            throw new TaskNotFoundException(title);
        }

        return TaskMapper.toDTO(task);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskDTO updateTask(Long id, CreateTaskRequest request) {
        log.info("Updating task {}", id);
        Task task = getTaskEntityById(id);
        TaskStatus previousStatus = task.getStatus();
        TaskMapper.updateEntity(task, request);

        Task savedTask = taskRepository.save(task);
        auditService.logAction(
                "UPDATE_TASK",
                "TASK",
                savedTask.getId(),
                "{\"title\":\"%s\"}".formatted(escapeJson(savedTask.getTitle())));
        if (previousStatus != savedTask.getStatus()) {
            auditService.logAction(
                    "STATUS_CHANGE",
                    "TASK",
                    savedTask.getId(),
                    "{\"before\":\"%s\",\"after\":\"%s\"}".formatted(previousStatus, savedTask.getStatus()));
        }
        log.info("Updated task {}", savedTask.getId());
        return TaskMapper.toDTO(savedTask);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteTask(Long id){
        log.info("Deleting task {}", id);
        Task task = getTaskEntityById(id);
        Long taskOwnerId = task.getUser().getId();
        String title = task.getTitle();
        taskRepository.delete(task);
        auditService.logAction(
                "DELETE_TASK",
                "TASK",
                id,
                "{\"title\":\"%s\",\"ownerUserId\":%d}".formatted(escapeJson(title), taskOwnerId));
        log.info("Deleted task {}", id);
    }

    private Task getTaskEntityById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private TaskStatus toStatus(String status) {
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid task status: " + status);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
