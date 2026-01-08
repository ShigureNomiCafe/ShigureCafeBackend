package cafe.shigure.ShigureCafeBackened.dto;

import lombok.Data;

@Data
public class UpdateEmailRequest {
    private String newEmail;
    private String verificationCode;
}
