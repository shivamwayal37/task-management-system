package com.portfolio.task_management_system.audit;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.portfolio.task_management_system.dto.AuditLogDTO;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogDTO> search(Long userId, String action, LocalDateTime from, LocalDateTime to,
            Pageable pageable) {
        return auditLogRepository.search(userId, action, from, to, pageable)
                .map(this::toDTO);
    }

    private AuditLogDTO toDTO(AuditLog auditLog) {
        return new AuditLogDTO(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getTimestamp(),
                auditLog.getDetails());
    }
}
