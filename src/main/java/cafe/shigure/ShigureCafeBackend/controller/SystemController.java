package cafe.shigure.ShigureCafeBackend.controller;

import cafe.shigure.ShigureCafeBackend.dto.SystemUpdatesResponse;
import cafe.shigure.ShigureCafeBackend.model.ReactionType;
import cafe.shigure.ShigureCafeBackend.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping("/reaction-types")
    public ResponseEntity<List<Map<String, String>>> getReactionTypes() {
        return ResponseEntity.ok(Arrays.stream(ReactionType.values())
                .map(type -> Map.of("name", type.name(), "emoji", type.getEmoji()))
                .collect(Collectors.toList()));
    }
}
