package com.portfolio.task_management_system.audit;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.entity.User;
import com.portfolio.task_management_system.repository.UserRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void logAction(String action, String entityType, Long entityId, String details) {
        logAction(getCurrentUserId(), action, entityType, entityId, details);
    }

    public void logAction(Long userId, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        User user = userRepository.findByName(username);
        return user == null ? null : user.getId();
    }
}
