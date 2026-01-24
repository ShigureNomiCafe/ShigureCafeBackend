package cafe.shigure.ShigureCafeBackend.dto;

import java.io.Serializable;

public record ChatMessageResponse(
    Long id,
    String name,
    String message,
    long timestamp
) implements Serializable {
}