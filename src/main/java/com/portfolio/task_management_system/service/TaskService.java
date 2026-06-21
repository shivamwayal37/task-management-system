package com.portfolio.task_management_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskStatus;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.event.TaskEventPublisher;
import com.portfolio.task_management_system.event.TaskUpdatedEvent;
import com.portfolio.task_management_system.exception.TaskNotFoundException;
import com.portfolio.task_management_system.exception.TaskStateTransitionException;
import com.portfolio.task_management_system.exception.UserNotFoundException;
import com.portfolio.task_management_system.mapper.TaskMapper;
import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import java.util.Set;

@Service
@Slf4j
public class TaskService {
    private static final Set<String> ALLOWED_TASK_SORT_FIELDS = Set.of(
            "id", "title", "description", "status", "createdAt", "updatedAt");
    
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TaskEventPublisher taskEventPublisher;

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentUser(#userId, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    @Transactional
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
            publishTaskEvent(savedTask, "CREATE_TASK");
            log.info("Created task {} for user {}", savedTask.getId(), userId);
            return TaskMapper.toDTO(savedTask);
        } catch (RuntimeException ex) {
            log.error("Task creation failed for user {}", userId, ex);
            throw ex;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Cacheable(
            value = "tasks",
            key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString() + '-' + (#status == null ? 'ALL' : #status)")
    @Transactional(readOnly = true)
    public Page<TaskDTO> getTasks(Pageable pageable, String status) {
        Pageable sanitizedPageable = sanitizeTaskPageable(pageable);
        log.info("Fetching tasks page={} size={} sort={} status={}",
                sanitizedPageable.getPageNumber(),
                sanitizedPageable.getPageSize(),
                sanitizedPageable.getSort(),
                status);

        if (status == null || status.isBlank()) {
            return taskRepository.findAll(sanitizedPageable).map(TaskMapper::toDTO);
        }

        return taskRepository.findByStatus(toStatus(status), sanitizedPageable).map(TaskMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentUser(#userId, authentication.name)")
    @Transactional(readOnly = true)
    public Page<TaskDTO> getTasksByUserId(Long userId, Pageable pageable) {
        log.info("Fetching tasks for user {}", userId);
        Pageable sanitizedPageable = sanitizeTaskPageable(pageable);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return taskRepository.findByUserId(userId, pageable)
                .map(TaskMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        log.info("Fetching task {}", id);
        Task task = getTaskEntityById(id);
        return TaskMapper.toDTO(task);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public TaskDTO getTask(String title){
        log.info("Searching task by title {}", title);
        Task task = taskRepository.findWithUserByTitle(title);
        if (task == null) {
            throw new TaskNotFoundException(title);
        }

        return TaskMapper.toDTO(task);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    @Transactional
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
        publishTaskEvent(savedTask, "UPDATE_TASK");
        if (previousStatus != savedTask.getStatus()) {
            auditService.logAction(
                    "STATUS_CHANGE",
                    "TASK",
                    savedTask.getId(),
                    "{\"before\":\"%s\",\"after\":\"%s\"}".formatted(previousStatus, savedTask.getStatus()));
            publishTaskEvent(savedTask, "STATUS_CHANGE");
        }
        log.info("Updated task {}", savedTask.getId());
        return TaskMapper.toDTO(savedTask);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    @Transactional
    public TaskDTO updateTaskStatus(Long id, String status) {
        Task task = getTaskEntityById(id);
        validateTaskIsActive(task);
        TaskStatus newStatus = toStatus(status);
        validateStatusTransition(task.getStatus(), newStatus);

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(newStatus);
        Task savedTask = taskRepository.save(task);
        auditService.logAction(
                "STATUS_CHANGE",
                "TASK",
                savedTask.getId(),
                "{\"before\":\"%s\",\"after\":\"%s\"}".formatted(previousStatus, savedTask.getStatus()));
        publishTaskEvent(savedTask, "STATUS_CHANGE");

        return TaskMapper.toDTO(savedTask);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "tasks", allEntries = true)
    @Transactional
    public TaskDTO assignTask(Long taskId, Long userId) {
        Task task = getTaskEntityById(taskId);
        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Long previousUserId = task.getUser().getId();
        task.setUser(assignee);

        Task savedTask = taskRepository.save(task);
        auditService.logAction(
                "ASSIGN_TASK",
                "TASK",
                savedTask.getId(),
                "{\"beforeUserId\":%d,\"afterUserId\":%d}".formatted(previousUserId, assignee.getId()));
        publishTaskEvent(savedTask, "ASSIGN_TASK");

        return TaskMapper.toDTO(savedTask);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    @CacheEvict(value = "tasks", allEntries = true)
    @Transactional
    public void deleteTask(Long id){
        log.info("Deleting task {}", id);
        Task task = getTaskEntityById(id);
        Long taskOwnerId = task.getUser().getId();
        String title = task.getTitle();
        task.setDeletedBy(auditService.getCurrentUserId());
        taskRepository.saveAndFlush(task);
        taskRepository.delete(task);
        taskRepository.flush();
        auditService.logAction(
                "DELETE_TASK",
                "TASK",
                id,
                "{\"title\":\"%s\",\"ownerUserId\":%d}".formatted(escapeJson(title), taskOwnerId));
        publishTaskEvent(id, taskOwnerId, title, "DELETE_TASK");
        log.info("Deleted task {}", id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "tasks", allEntries = true)
    @Transactional
    public void restoreTask(Long id) {
        log.info("Restoring task {}", id);
        Task task = taskRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        if (!task.isDeleted()) {
            log.info("Task {} is already active", id);
            return;
        }

        taskRepository.restoreById(id);
        auditService.logAction(
                "RESTORE_TASK",
                "TASK",
                id,
                "{\"title\":\"%s\"}".formatted(escapeJson(task.getTitle())));
        publishTaskEvent(task, "RESTORE_TASK");
        log.info("Restored task {}", id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<TaskDTO> getDeletedTasks(Pageable pageable) {
        return taskRepository.findDeletedTasks(pageable).map(TaskMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void hardDeleteTask(Long id) {
        Task task = taskRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.hardDeleteById(id);
        auditService.logAction(
                "HARD_DELETE_TASK",
                "TASK",
                id,
                "{\"title\":\"%s\"}".formatted(escapeJson(task.getTitle())));
    }

    private Task getTaskEntityById(Long id) {
        return taskRepository.findWithUserById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private TaskStatus toStatus(String status) {
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid task status: " + status);
        }
    }

    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean valid = switch (currentStatus) {
            case TODO -> newStatus == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == TaskStatus.COMPLETED;
            case COMPLETED -> false;
        };

        if (!valid) {
            throw new TaskStateTransitionException(
                    "Invalid task status transition: %s -> %s".formatted(currentStatus, newStatus));
        }
    }

    private void validateTaskIsActive(Task task) {
        if (task.isDeleted()) {
            throw new TaskStateTransitionException(
                    "Cannot update status for deleted task: %d".formatted(task.getId()));
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void publishTaskEvent(Task task, String updateType) {
        publishTaskEvent(task.getId(), task.getUser().getId(), task.getTitle(), updateType);
    }

    private void publishTaskEvent(Long taskId, Long userId, String taskTitle, String updateType) {
        TaskUpdatedEvent event = TaskUpdatedEvent.builder()
                .version(1)
                .taskId(taskId)
                .userId(userId)
                .taskTitle(taskTitle)
                .updateType(updateType)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        taskEventPublisher.publishAfterCommit(event);
    }

    private Pageable sanitizeTaskPageable(Pageable pageable) {
        Sort sanitizedSort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            if (ALLOWED_TASK_SORT_FIELDS.contains(order.getProperty())) {
                sanitizedSort = sanitizedSort.and(Sort.by(new Sort.Order(order.getDirection(), order.getProperty())));
            }
        }

        if (sanitizedSort.isUnsorted()) {
            sanitizedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sanitizedSort);
    }
}
