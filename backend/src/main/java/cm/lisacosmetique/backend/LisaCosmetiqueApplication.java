package cm.lisacosmetique.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// L'utilisateur en mémoire généré par défaut est désactivé : l'authentification
// s'appuie sur la table users (HTTP Basic pour l'instant, JWT prévu ensuite).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
// Le stockage enregistre ses propriétés lui-même, seulement s'il est activé (voir StorageConfig).
@ConfigurationPropertiesScan("cm.lisacosmetique.backend.config")
public class LisaCosmetiqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(LisaCosmetiqueApplication.class, args);
    }
}
