package cafe.shigure.ShigureCafeBackend.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String verificationCode;
    private String newPassword;
    private String turnstileToken;
}
