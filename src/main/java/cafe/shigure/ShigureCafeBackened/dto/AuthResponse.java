package cafe.shigure.ShigureCafeBackened.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private boolean twoFactorRequired;
    private String email;

    public AuthResponse(String token) {
        this.token = token;
        this.twoFactorRequired = false;
        this.email = null;
    }
}
