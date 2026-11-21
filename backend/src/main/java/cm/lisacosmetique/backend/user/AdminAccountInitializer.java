package cm.lisacosmetique.backend.user;

import cm.lisacosmetique.backend.config.AdminBootstrapProperties;
import cm.lisacosmetique.backend.user.entity.Role;
import cm.lisacosmetique.backend.user.entity.User;
import cm.lisacosmetique.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crée le premier ADMIN si ADMIN_EMAIL / ADMIN_PASSWORD sont fournis et que le
 * compte n'existe pas. Un compte existant n'est jamais modifié.
 */
@Component
@Profile("!nodb")
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(AdminBootstrapProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("Amorçage ADMIN désactivé (ADMIN_EMAIL / ADMIN_PASSWORD non renseignés).");
            return;
        }
        if (properties.password().length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD doit contenir au moins 8 caractères.");
        }
        String email = User.normalizeEmail(properties.email());
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Compte ADMIN déjà présent : aucun changement.");
            return;
        }
        userRepository.save(new User("Admin", "Lisa", email, null,
                passwordEncoder.encode(properties.password()), Role.ADMIN));
        log.info("Compte ADMIN initial créé pour {}.", email);
    }
}
