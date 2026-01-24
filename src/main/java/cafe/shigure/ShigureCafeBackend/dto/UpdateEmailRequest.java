package cafe.shigure.ShigureCafeBackend.dto;

import lombok.Data;

@Data
public class UpdateEmailRequest {
    private String newEmail;
    private String verificationCode;
}
