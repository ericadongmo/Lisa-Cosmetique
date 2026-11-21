package cm.lisacosmetique.backend.common.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Audit JPA (createdAt / updatedAt / createdBy / updatedBy). */
@Configuration
@Profile("!nodb")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Aucun utilisateur authentifié n'existe encore : les colonnes created_by /
     * updated_by restent nulles jusqu'à la phase d'authentification.
     */
    @Bean
    AuditorAware<UUID> auditorProvider() {
        return Optional::empty;
    }
}
