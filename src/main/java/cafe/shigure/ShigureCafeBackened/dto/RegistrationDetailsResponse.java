package cafe.shigure.ShigureCafeBackened.dto;

import cafe.shigure.ShigureCafeBackened.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationDetailsResponse {
    private String username;
    private String nickname;
    private String email;
    private UserStatus status;
    private String auditCode;
    private boolean isExpired;
}
