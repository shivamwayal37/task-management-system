package com.portfolio.task_management_system.service;

import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.repository.TaskRepository;
import com.portfolio.task_management_system.repository.UserRepository;

@Service("authorizationService")
public class AuthorizationService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public AuthorizationService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public boolean isCurrentUser(Long userId, String username) {
        return userRepository.findById(userId)
                .map(user -> user.getName().equals(username))
                .orElse(false);
    }

    public boolean isTaskOwner(Long taskId, String username) {
        return taskRepository.findById(taskId)
                .map(task -> task.getUser().getName().equals(username))
                .orElse(false);
    }
}
