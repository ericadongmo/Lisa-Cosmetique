package cm.lisacosmetique.backend.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param url           URL du projet Supabase (https://&lt;project-ref&gt;.supabase.co)
 * @param secretKey     clé secrète serveur (sb_secret_… ou ancienne clé service_role).
 *                      Elle contourne les règles RLS : elle ne doit jamais quitter le backend.
 * @param bucket        bucket privé contenant les fichiers de la boutique
 * @param signedUrlTtl  durée de validité des URLs signées
 */
@Validated
@ConfigurationProperties(prefix = "app.storage.supabase")
public record SupabaseStorageProperties(
        @NotBlank String url,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @NotNull Duration signedUrlTtl) {
}
