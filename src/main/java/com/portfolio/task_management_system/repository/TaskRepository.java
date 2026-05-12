package com.portfolio.task_management_system.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.task_management_system.entity.Task;
import com.portfolio.task_management_system.entity.TaskStatus;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>{
    Task findByTitle(String title);

    @EntityGraph(attributePaths = "user")
    Optional<Task> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    Task findWithUserByTitle(String title);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Task> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    @Query(value = "SELECT * FROM tasks WHERE id = :id", nativeQuery = true)
    Optional<Task> findByIdIncludingDeleted(@Param("id") Long id);

    @Modifying
    @Query(value = """
            UPDATE tasks
            SET deleted = false,
                deleted_at = NULL,
                deleted_by = NULL
            WHERE id = :id
            """, nativeQuery = true)
    int restoreById(@Param("id") Long id);
}
