package cafe.shigure.ShigureCafeBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record ChatMessageRequest(
    @NotBlank
    @Size(max = 32)
    String name,

    @NotBlank
    String message,

    @NotNull
    Long timestamp
) {
}