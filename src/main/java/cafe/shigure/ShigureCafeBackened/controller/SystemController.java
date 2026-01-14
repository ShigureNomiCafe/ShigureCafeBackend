package cafe.shigure.ShigureCafeBackened.controller;

import cafe.shigure.ShigureCafeBackened.dto.SystemUpdatesResponse;
import cafe.shigure.ShigureCafeBackened.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final CacheService cacheService;

    @GetMapping("/updates")
    public ResponseEntity<SystemUpdatesResponse> getUpdates() {
        return ResponseEntity.ok(new SystemUpdatesResponse(
                cacheService.getTimestamp(CacheService.NOTICE_LIST_KEY),
                cacheService.getTimestamp(CacheService.USER_LIST_KEY),
                cacheService.getTimestamp(CacheService.AUDIT_LIST_KEY)
        ));
    }
}
