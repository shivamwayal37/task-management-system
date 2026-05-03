package com.portfolio.task_management_system.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }

    public TaskNotFoundException(String title) {
        super("Task not found with title: " + title);
    }
}
