package com.portfolio.task_management_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.portfolio.task_management_system.audit.AuditService;
import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskStatus;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.event.TaskEventPublisher;
import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

@SpringJUnitConfig(TaskServiceCachingTest.CachingTestConfig.class)
class TaskServiceCachingTest {

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        TaskService taskService() {
            return new TaskService();
        }

        @Bean
        CountingCacheManager cacheManager() {
            return new CountingCacheManager();
        }
    }

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private TaskEventPublisher taskEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    private TaskService taskService;

    @org.springframework.beans.factory.annotation.Autowired
    private CountingCacheManager cacheManager;

    @BeforeEach
    void resetCacheAndMocks() {
        cacheManager.tasksCache.clear();
        cacheManager.tasksCache.resetCounters();
        Mockito.reset(taskRepository, userRepository, auditService, taskEventPublisher);
    }

    @Test
    void getTasksCacheMissFallsBackToRepositoryAndCachesResult() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Task task = task(101L, TaskStatus.TODO, 11L);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskDTO> first = taskService.getTasks(pageable, null);
        Page<TaskDTO> second = taskService.getTasks(pageable, null);

        assertThat(first.getTotalElements()).isEqualTo(1);
        assertThat(second.getTotalElements()).isEqualTo(1);
        verify(taskRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void updateTaskStatusEvictsTasksCache() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Task task = task(201L, TaskStatus.TODO, 15L);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task)));
        when(taskRepository.findWithUserById(201L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.getTasks(pageable, null);
        assertThat(cacheManager.tasksCache.size()).isEqualTo(1);

        taskService.updateTaskStatus(201L, "IN_PROGRESS");

        assertThat(cacheManager.tasksCache.clearCount()).isEqualTo(1);
        assertThat(cacheManager.tasksCache.size()).isEqualTo(0);
    }

    @Test
    void updateTaskEvictsTasksCache() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Task task = task(301L, TaskStatus.TODO, 19L);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task)));
        when(taskRepository.findWithUserById(301L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.getTasks(pageable, null);
        assertThat(cacheManager.tasksCache.size()).isEqualTo(1);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Updated title");
        request.setDescription("Updated description");
        request.setStatus("IN_PROGRESS");
        taskService.updateTask(301L, request);

        assertThat(cacheManager.tasksCache.clearCount()).isEqualTo(1);
        assertThat(cacheManager.tasksCache.size()).isEqualTo(0);
    }

    @Test
    void deleteTaskEvictsTasksCache() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Task task = task(401L, TaskStatus.TODO, 23L);
        when(taskRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task)));
        when(taskRepository.findWithUserById(401L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditService.getCurrentUserId()).thenReturn(999L);

        taskService.getTasks(pageable, null);
        assertThat(cacheManager.tasksCache.size()).isEqualTo(1);

        taskService.deleteTask(401L);

        assertThat(cacheManager.tasksCache.clearCount()).isEqualTo(1);
        assertThat(cacheManager.tasksCache.size()).isEqualTo(0);
    }

    private Task task(Long taskId, TaskStatus status, Long ownerId) {
        User user = new User();
        user.setId(ownerId);

        Task task = new Task();
        task.setId(taskId);
        task.setTitle("Task " + taskId);
        task.setDescription("Cache test task");
        task.setStatus(status);
        task.setUser(user);
        task.setDeleted(false);
        return task;
    }

    static class CountingCacheManager extends SimpleCacheManager {
        private final CountingConcurrentMapCache tasksCache = new CountingConcurrentMapCache("tasks");

        CountingCacheManager() {
            setCaches(List.of(tasksCache));
            initializeCaches();
        }

        @Override
        public Cache getCache(String name) {
            return super.getCache(name);
        }
    }

    static class CountingConcurrentMapCache extends ConcurrentMapCache {
        private int clearCount;

        CountingConcurrentMapCache(String name) {
            super(name);
        }

        @Override
        public void clear() {
            clearCount++;
            super.clear();
        }

        int clearCount() {
            return clearCount;
        }

        int size() {
            return getNativeCache().size();
        }

        void resetCounters() {
            clearCount = 0;
        }
    }
}
