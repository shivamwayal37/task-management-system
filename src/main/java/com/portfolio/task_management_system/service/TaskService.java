package com.portfolio.task_management_system.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.dto.CreateTaskRequest;
import com.portfolio.task_management_system.dto.TaskDTO;
import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.exception.TaskNotFoundException;
import com.portfolio.task_management_system.exception.UserNotFoundException;
import com.portfolio.task_management_system.mapper.TaskMapper;
import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentUser(#userId, authentication.name)")
    public TaskDTO createTask(Long userId, CreateTaskRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Task task = TaskMapper.toEntity(request);
        task.setUser(user);
        Task savedTask = taskRepository.save(task);
        return TaskMapper.toDTO(savedTask);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(TaskMapper::toDTO)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCurrentUser(#userId, authentication.name)")
    public Page<TaskDTO> getTasksByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return taskRepository.findByUserId(userId, pageable)
                .map(TaskMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    public TaskDTO getTaskById(Long id) {
        Task task = getTaskEntityById(id);
        return TaskMapper.toDTO(task);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public TaskDTO getTask(String title){
        Task task = taskRepository.findByTitle(title);
        if (task == null) {
            throw new TaskNotFoundException(title);
        }

        return TaskMapper.toDTO(task);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    public TaskDTO updateTask(Long id, CreateTaskRequest request) {
        Task task = getTaskEntityById(id);
        TaskMapper.updateEntity(task, request);

        return TaskMapper.toDTO(taskRepository.save(task));
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isTaskOwner(#id, authentication.name)")
    public void deleteTask(Long id){
        Task task = getTaskEntityById(id);
        taskRepository.delete(task);
    }

    private Task getTaskEntityById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
