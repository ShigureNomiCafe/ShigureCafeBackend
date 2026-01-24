package cafe.shigure.ShigureCafeBackend.controller;

import cafe.shigure.ShigureCafeBackend.annotation.RateLimit;
import cafe.shigure.ShigureCafeBackend.dto.PagedResponse;
import cafe.shigure.ShigureCafeBackend.dto.RegisterRequest;
import cafe.shigure.ShigureCafeBackend.dto.RegistrationDetailsResponse;
import cafe.shigure.ShigureCafeBackend.exception.BusinessException;
import cafe.shigure.ShigureCafeBackend.model.User;
import cafe.shigure.ShigureCafeBackend.service.RateLimitService;
import cafe.shigure.ShigureCafeBackend.service.TurnstileService;
import cafe.shigure.ShigureCafeBackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final TurnstileService turnstileService;

    @PostMapping
    @RateLimit(key = "register", useIp = true, milliseconds = 5000)
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        if (!turnstileService.verifyToken(request.getTurnstileToken())) {
            throw new BusinessException("INVALID_CAPTCHA");
        }
        String auditCode = userService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/registrations/" + auditCode))
                .body(Map.of("auditCode", auditCode));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @RateLimit(key = "audits:list", expression = "#currentUser.id", milliseconds = 500)
    public ResponseEntity<PagedResponse<RegistrationDetailsResponse>> getAllRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("user.username").ascending());
        return ResponseEntity.ok(userService.getAuditsPaged(pageable));
    }

    @GetMapping("/{auditCode}")
    public ResponseEntity<RegistrationDetailsResponse> checkRegistration(@PathVariable String auditCode) {
        return ResponseEntity.ok(userService.getRegistrationDetails(auditCode));
    }

    @PatchMapping("/{auditCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> approveRegistration(@PathVariable String auditCode) {
        userService.approveUser(auditCode);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{auditCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> banUser(@PathVariable String auditCode) {
        userService.banUser(auditCode);
        return ResponseEntity.ok().build();
    }
}
