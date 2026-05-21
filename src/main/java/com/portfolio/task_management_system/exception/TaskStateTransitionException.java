package com.portfolio.task_management_system.exception;

public class TaskStateTransitionException extends IllegalArgumentException {

    public TaskStateTransitionException(String message) {
        super(message);
    }
}
