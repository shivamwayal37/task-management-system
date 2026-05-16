package com.portfolio.task_management_system.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.dto.CreateCommentRequest;
import com.portfolio.task_management_system.dto.TaskCommentDTO;
import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskComment;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.event.TaskEventPublisher;
import com.portfolio.task_management_system.event.TaskUpdatedEvent;
import com.portfolio.task_management_system.exception.TaskNotFoundException;
import com.portfolio.task_management_system.exception.UserNotFoundException;
import com.portfolio.task_management_system.mapper.TaskCommentMapper;
import com.portfolio.task_management_system.repository.TaskCommentRepository;
import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

@Service
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TaskEventPublisher taskEventPublisher;

    public TaskCommentService(TaskCommentRepository taskCommentRepository, TaskRepository taskRepository,
            UserRepository userRepository, AuditService auditService, TaskEventPublisher taskEventPublisher) {
        this.taskCommentRepository = taskCommentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.taskEventPublisher = taskEventPublisher;
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#taskId, authentication.name)")
    @Transactional
    public TaskCommentDTO createComment(Long taskId, CreateCommentRequest request) {
        Task task = taskRepository.findWithUserById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        Long currentUserId = auditService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setContent(request.getContent());
        TaskComment savedComment = taskCommentRepository.save(comment);

        auditService.logAction(
                "CREATE_COMMENT",
                "TASK",
                taskId,
                "{\"commentId\":%d}".formatted(savedComment.getId()));
        taskEventPublisher.publishAfterCommit(TaskUpdatedEvent.builder()
                .version(1)
                .taskId(task.getId())
                .userId(task.getUser().getId())
                .taskTitle(task.getTitle())
                .updateType("COMMENT_ADDED")
                .timestamp(java.time.LocalDateTime.now())
                .build());

        return TaskCommentMapper.toDTO(savedComment);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#taskId, authentication.name)")
    @Transactional(readOnly = true)
    public List<TaskCommentDTO> getComments(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }

        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(TaskCommentMapper::toDTO)
                .toList();
    }
}
