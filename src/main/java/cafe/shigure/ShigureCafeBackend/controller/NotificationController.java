package cafe.shigure.ShigureCafeBackend.controller;

import cafe.shigure.ShigureCafeBackend.dto.EmailNotificationRequest;
import cafe.shigure.ShigureCafeBackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final UserService userService;

    @PostMapping("/email/all-active")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> sendEmailToAllActiveUsers(@Valid @RequestBody EmailNotificationRequest request) {
        userService.sendEmailNotificationToAllActiveUsers(request.getSubject(), request.getContent());

        return ResponseEntity.accepted().body(Map.of("message", "Email notification process started"));
    }
}
