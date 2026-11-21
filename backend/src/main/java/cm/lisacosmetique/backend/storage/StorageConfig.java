package cm.lisacosmetique.backend.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Le stockage n'est activé que lorsque la clé secrète Supabase est renseignée,
 * ce qui permet de démarrer l'API avant la création du projet Supabase.
 */
@Configuration
@ConditionalOnExpression("!'${app.storage.supabase.secret-key:}'.isBlank()")
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class StorageConfig {

    @Bean
    StorageService storageService(SupabaseStorageProperties properties, RestClient.Builder restClientBuilder) {
        return new SupabaseStorageService(properties, restClientBuilder);
    }
}
