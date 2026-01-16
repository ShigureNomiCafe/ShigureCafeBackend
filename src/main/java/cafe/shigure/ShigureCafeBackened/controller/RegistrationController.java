package cafe.shigure.ShigureCafeBackened.controller;

import cafe.shigure.ShigureCafeBackened.annotation.RateLimit;
import cafe.shigure.ShigureCafeBackened.dto.PagedResponse;
import cafe.shigure.ShigureCafeBackened.dto.RegisterRequest;
import cafe.shigure.ShigureCafeBackened.dto.RegistrationDetailsResponse;
import cafe.shigure.ShigureCafeBackened.model.User;
import cafe.shigure.ShigureCafeBackened.service.RateLimitService;
import cafe.shigure.ShigureCafeBackened.service.UserService;
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

    @PostMapping
    @RateLimit(key = "register", useIp = true, milliseconds = 5000)
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
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
