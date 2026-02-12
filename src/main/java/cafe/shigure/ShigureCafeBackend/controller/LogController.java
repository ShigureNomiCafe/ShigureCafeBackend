package cafe.shigure.ShigureCafeBackend.controller;

import cafe.shigure.ShigureCafeBackend.dto.LogRequest;
import cafe.shigure.ShigureCafeBackend.model.SystemLog;
import cafe.shigure.ShigureCafeBackend.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {
    private final LogService logService;

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.List<SystemLog>> getLatestLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long afterId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(logService.getLogsAfter(level, source, search, afterId, limit));
    }

    @GetMapping("/older")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.List<SystemLog>> getOlderLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search,
            @RequestParam Long beforeId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(logService.getLogsBefore(level, source, search, beforeId, limit));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('API_CLIENT')")
    public ResponseEntity<Void> addLog(@RequestBody LogRequest request) {
        logService.log(
                request.getLevel() != null ? request.getLevel().toUpperCase() : "INFO",
                request.getSource() != null ? request.getSource() : "EXTERNAL",
                request.getContent(),
                request.getTimestamp()
        );
        return ResponseEntity.ok().build();
    }
}
