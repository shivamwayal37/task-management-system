package com.portfolio.task_management_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskStatus;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.event.TaskEventPublisher;
import com.portfolio.task_management_system.event.TaskUpdatedEvent;
import com.portfolio.task_management_system.exception.TaskStateTransitionException;
import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceStateEngineTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private TaskEventPublisher taskEventPublisher;

    @InjectMocks
    private TaskService taskService;

    @Test
    void updateTaskStatusAllowsTodoToInProgress() {
        Task task = task(1L, TaskStatus.TODO, false);
        when(taskRepository.findWithUserById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDTO response = taskService.updateTaskStatus(1L, "IN_PROGRESS");

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).save(task);
        verify(auditService).logAction(
                eq("STATUS_CHANGE"),
                eq("TASK"),
                eq(1L),
                eq("{\"before\":\"TODO\",\"after\":\"IN_PROGRESS\"}"));
        verify(taskEventPublisher).publishAfterCommit(any(TaskUpdatedEvent.class));
    }

    @Test
    void updateTaskStatusAllowsInProgressToCompleted() {
        Task task = task(2L, TaskStatus.IN_PROGRESS, false);
        when(taskRepository.findWithUserById(2L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDTO response = taskService.updateTaskStatus(2L, "COMPLETED");

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        verify(taskRepository).save(task);
        verify(auditService).logAction(
                eq("STATUS_CHANGE"),
                eq("TASK"),
                eq(2L),
                eq("{\"before\":\"IN_PROGRESS\",\"after\":\"COMPLETED\"}"));
        verify(taskEventPublisher).publishAfterCommit(any(TaskUpdatedEvent.class));
    }

    @Test
    void updateTaskStatusRejectsTodoToCompleted() {
        Task task = task(3L, TaskStatus.TODO, false);
        when(taskRepository.findWithUserById(3L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTaskStatus(3L, "COMPLETED"))
                .isInstanceOf(TaskStateTransitionException.class)
                .hasMessage("Invalid task status transition: TODO -> COMPLETED");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(auditService, taskEventPublisher);
    }

    @Test
    void updateTaskStatusRejectsDeletedTask() {
        Task task = task(4L, TaskStatus.TODO, true);
        when(taskRepository.findWithUserById(4L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTaskStatus(4L, "IN_PROGRESS"))
                .isInstanceOf(TaskStateTransitionException.class)
                .hasMessage("Cannot update status for deleted task: 4");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(auditService, taskEventPublisher);
    }

    private Task task(Long id, TaskStatus status, boolean deleted) {
        User user = new User();
        user.setId(10L);

        Task task = new Task();
        task.setId(id);
        task.setTitle("State engine test task");
        task.setDescription("Task used for transition tests");
        task.setStatus(status);
        task.setUser(user);
        task.setDeleted(deleted);
        return task;
    }
}
