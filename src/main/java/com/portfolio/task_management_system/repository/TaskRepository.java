package com.portfolio.task_management_system.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long>{
    Task findByTitle(String title);

    Page<Task> findByUserId(Long userId, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
