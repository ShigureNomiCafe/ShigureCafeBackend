package cafe.shigure.ShigureCafeBackend.controller;

import cafe.shigure.ShigureCafeBackend.annotation.RateLimit;
import cafe.shigure.ShigureCafeBackend.dto.*;
import cafe.shigure.ShigureCafeBackend.model.User;
import cafe.shigure.ShigureCafeBackend.service.MinecraftService;
import cafe.shigure.ShigureCafeBackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/minecraft")
@RequiredArgsConstructor
public class MinecraftController {

    private final UserService userService;
    private final MinecraftService minecraftService;

    @GetMapping("/whitelist")
    public ResponseEntity<List<MinecraftWhitelistResponse>> getWhitelist() {
        return ResponseEntity.ok(userService.getMinecraftWhitelist());
    }

    @GetMapping("/chat")
    @RateLimit(key = "minecraft:chat", expression = "#currentUser.id", period = 500, capacity = 10)
    public ResponseEntity<PagedResponse<ChatMessageResponse>> getChatMessages(
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(minecraftService.getChatMessages(pageable));
    }
}
