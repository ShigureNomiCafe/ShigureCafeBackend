package cafe.shigure.UserService.dto;

import cafe.shigure.UserService.model.UserStatus;

public record AuditResponse(
    Long userId,
    String username,
    String email,
    UserStatus status,
    String auditCode,
    boolean isExpired
) {}
