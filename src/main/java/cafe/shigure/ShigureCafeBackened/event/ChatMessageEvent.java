package cafe.shigure.ShigureCafeBackened.event;

import cafe.shigure.ShigureCafeBackened.dto.ChatMessageResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChatMessageEvent extends ApplicationEvent {
    private final ChatMessageResponse message;
    private final String senderSessionId;

    public ChatMessageEvent(Object source, ChatMessageResponse message) {
        this(source, message, null);
    }

    public ChatMessageEvent(Object source, ChatMessageResponse message, String senderSessionId) {
        super(source);
        this.message = message;
        this.senderSessionId = senderSessionId;
    }
}
