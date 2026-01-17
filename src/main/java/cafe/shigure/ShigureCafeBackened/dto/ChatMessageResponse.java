package cafe.shigure.ShigureCafeBackened.dto;

import java.io.Serializable;

public record ChatMessageResponse(
    Long id,
    String name,
    String message,
    long timestamp
) implements Serializable {
}