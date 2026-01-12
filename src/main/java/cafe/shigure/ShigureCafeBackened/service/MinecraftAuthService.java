package cafe.shigure.ShigureCafeBackened.service;

import cafe.shigure.ShigureCafeBackened.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MinecraftAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${application.microsoft.minecraft.client-id}")
    private String clientId;

    @Value("${application.microsoft.minecraft.client-secret}")
    private String clientSecret;

    @Value("${application.microsoft.minecraft.tenant-id}")
    private String tenantId;

    public String getMinecraftUuid(String code, String redirectUri) {
        // 1. Exchange code for Microsoft Access Token
        String msAccessToken = getMicrosoftAccessToken(code, redirectUri);

        // 2. Exchange MS Access Token for Xbox Live Token
        Map<String, String> xboxAuth = getXboxLiveToken(msAccessToken);
        String xboxToken = xboxAuth.get("token");
        String uhs = xboxAuth.get("uhs");

        // 3. Exchange Xbox Live Token for XSTS Token
        String xstsToken = getXstsToken(xboxToken);

        // 4. Exchange XSTS Token for Minecraft Access Token
        String mcAccessToken = getMinecraftAccessToken(xstsToken, uhs);

        // 5. Get Minecraft Profile
        return getMinecraftProfile(mcAccessToken);
    }

    private String getMicrosoftAccessToken(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectUri);
        map.add("scope", "XboxLive.signin offline_access");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        // Using the modern v2.0 consumers endpoint
        String tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Microsoft Auth Error: " + e.getResponseBodyAsString());
            throw new BusinessException("MICROSOFT_AUTH_FAILED");
        }
        throw new BusinessException("MICROSOFT_AUTH_FAILED");
    }

    private Map<String, String> getXboxLiveToken(String msAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        Map<String, Object> body = Map.of(
                "Properties", Map.of(
                        "AuthMethod", "RPS",
                        "SiteName", "user.auth.xboxlive.com",
                        "RpsTicket", "d=" + msAccessToken
                ),
                "RelyingParty", "http://auth.xboxlive.com",
                "TokenType", "JWT"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity("https://user.auth.xboxlive.com/user/authenticate", request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String token = (String) response.getBody().get("Token");
                Map displayClaims = (Map) response.getBody().get("DisplayClaims");
                List xui = (List) displayClaims.get("xui");
                Map firstXui = (Map) xui.get(0);
                String uhs = (String) firstXui.get("uhs");
                return Map.of("token", token, "uhs", uhs);
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Xbox Live Auth Error: " + e.getResponseBodyAsString());
            throw new BusinessException("XBOX_LIVE_AUTH_FAILED");
        }
        throw new BusinessException("XBOX_LIVE_AUTH_FAILED");
    }

    private String getXstsToken(String xboxToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        Map<String, Object> body = Map.of(
                "Properties", Map.of(
                        "SandboxId", "RETAIL",
                        "UserTokens", List.of(xboxToken)
                ),
                "RelyingParty", "rp://api.minecraftservices.com/",
                "TokenType", "JWT"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity("https://xsts.auth.xboxlive.com/xsts/authorize", request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("Token");
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            System.err.println("XSTS Auth Error: " + responseBody);
            // Check for common Xbox errors
            if (responseBody.contains("2148916233")) throw new BusinessException("XBOX_ACCOUNT_NOT_FOUND");
            if (responseBody.contains("2148916238")) throw new BusinessException("XBOX_CHILD_ACCOUNT_RESTRICTION");
            throw new BusinessException("XSTS_AUTH_FAILED");
        }
        throw new BusinessException("XSTS_AUTH_FAILED");
    }

    private String getMinecraftAccessToken(String xstsToken, String uhs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        Map<String, Object> body = Map.of(
                "identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity("https://api.minecraftservices.com/authentication/login_with_xbox", request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            System.err.println("Minecraft Login Error: " + responseBody);
            if (responseBody.contains("Invalid app registration")) {
                throw new BusinessException("MINECRAFT_APP_REGISTRATION_INVALID");
            }
            throw new BusinessException("MINECRAFT_AUTH_FAILED");
        }
        throw new BusinessException("MINECRAFT_AUTH_FAILED");
    }

    private String getMinecraftProfile(String mcAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mcAccessToken);
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange("https://api.minecraftservices.com/minecraft/profile", HttpMethod.GET, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("id");
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Minecraft Profile Error: " + e.getResponseBodyAsString());
            throw new BusinessException("MINECRAFT_PROFILE_FAILED");
        }
        throw new BusinessException("MINECRAFT_PROFILE_FAILED");
    }
}
