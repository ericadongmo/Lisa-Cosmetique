package cm.lisacosmetique.backend.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point de contrôle léger utilisé par le frontend pour vérifier que l'API répond.
 * L'état détaillé de l'infrastructure (base, disque…) est exposé par /actuator/health.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Disponibilité de l'API")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    @Operation(summary = "Vérifie que l'API est disponible")
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName, Instant.now());
    }
}
