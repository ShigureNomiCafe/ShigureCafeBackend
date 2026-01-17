package cafe.shigure.ShigureCafeBackened.dto;

import java.util.List;

public record ChatSyncRequest(
    List<ChatMessageRequest> messages,
    Long lastTimestamp
) {
}