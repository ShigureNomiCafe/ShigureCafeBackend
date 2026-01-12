package cafe.shigure.ShigureCafeBackened.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {

    @Value("${application.microsoft.mail.client-id}")
    private String clientId;

    @Value("${application.microsoft.mail.client-secret}")
    private String clientSecret;

    @Value("${application.microsoft.mail.tenant-id}")
    private String tenantId;

    @Bean
    public GraphServiceClient graphServiceClient() {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        return new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
    }
}
