package cafe.shigure.ShigureCafeBackened.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String verificationCode;
    private String newPassword;
}
