package cafe.shigure.ShigureCafeBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private boolean twoFactorRequired;
    private boolean hasTotp;
    private boolean hasEmail2fa;
    private String email;

    public AuthResponse(String token) {
        this.token = token;
        this.twoFactorRequired = false;
        this.hasTotp = false;
        this.hasEmail2fa = false;
        this.email = null;
    }
}
