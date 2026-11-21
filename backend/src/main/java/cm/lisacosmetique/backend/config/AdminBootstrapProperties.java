package cm.lisacosmetique.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Amorce un compte ADMIN au premier démarrage avec base. Une fois le compte
 * créé, ces variables peuvent être retirées : le mot de passe n'est jamais
 * écrasé.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminBootstrapProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank()
                && password != null && !password.isBlank();
    }
}
