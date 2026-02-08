package cafe.shigure.ShigureCafeBackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {
    @NotBlank
    private String subject;

    @NotBlank
    private String content;
}
