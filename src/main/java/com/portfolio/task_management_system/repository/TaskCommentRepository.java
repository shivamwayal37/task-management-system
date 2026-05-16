package com.portfolio.task_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.portfolio.task_management_system.entity.TaskComment;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = {"task", "user"})
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
