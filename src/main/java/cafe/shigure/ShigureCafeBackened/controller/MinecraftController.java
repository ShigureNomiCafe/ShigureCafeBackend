package cafe.shigure.ShigureCafeBackened.controller;

import cafe.shigure.ShigureCafeBackened.dto.ChatMessageRequest;
import cafe.shigure.ShigureCafeBackened.dto.ChatMessageResponse;
import cafe.shigure.ShigureCafeBackened.dto.ChatSyncRequest;
import cafe.shigure.ShigureCafeBackened.dto.MinecraftWhitelistResponse;
import cafe.shigure.ShigureCafeBackened.service.MinecraftService;
import cafe.shigure.ShigureCafeBackened.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/message-sync")
    public ResponseEntity<List<ChatMessageResponse>> syncChatMessages(@Valid @RequestBody ChatSyncRequest request) {
        return ResponseEntity.ok(minecraftService.syncChatMessages(request));
    }
}
