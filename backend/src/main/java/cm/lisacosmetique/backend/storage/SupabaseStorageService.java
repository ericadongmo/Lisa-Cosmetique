package cm.lisacosmetique.backend.storage;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Implémentation sur l'API REST de Supabase Storage (/storage/v1). Déclarée dans {@link StorageConfig}.
 */
public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final SupabaseStorageProperties properties;
    private final String storageBaseUrl;
    private final RestClient client;

    public SupabaseStorageService(SupabaseStorageProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.storageBaseUrl = properties.url().replaceAll("/+$", "") + "/storage/v1";
        this.client = restClientBuilder
                .baseUrl(storageBaseUrl)
                .defaultHeader("apikey", properties.secretKey())
                .defaultHeader("Authorization", "Bearer " + properties.secretKey())
                .build();
    }

    /**
     * Crée le bucket (privé) au démarrage s'il n'existe pas. Si Supabase Storage est
     * injoignable ou non configuré, l'API démarre quand même : seules les fonctions fichiers échoueront.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        String bucket = properties.bucket();
        try {
            client.get().uri("/bucket/{id}", bucket).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            if (!isBucketNotFound(ex)) {
                log.warn("Supabase Storage a refusé l'accès au bucket '{}' ({}).", bucket, ex.getStatusCode());
                return;
            }
            createPrivateBucket(bucket);
        } catch (RestClientException ex) {
            log.warn("Supabase Storage injoignable ({}). Le stockage de fichiers est indisponible.",
                    ex.getMessage());
        }
    }

    @Override
    public void upload(String objectKey, InputStream content, long size, String contentType) {
        try {
            client.post()
                    .uri(objectUri("/object", objectKey))
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(size)
                    .header("x-upsert", "true")
                    .body(new InputStreamResource(content))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new StorageException("Échec de l'envoi du fichier " + objectKey, ex);
        }
    }

    @Override
    public String presignedDownloadUrl(String objectKey) {
        try {
            SignedUrlResponse response = client.post()
                    .uri(objectUri("/object/sign", objectKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", properties.signedUrlTtl().toSeconds()))
                    .retrieve()
                    .body(SignedUrlResponse.class);
            if (response == null || response.signedURL() == null) {
                throw new StorageException("Réponse de signature vide pour " + objectKey, null);
            }
            // Supabase renvoie un chemin relatif à /storage/v1.
            return storageBaseUrl + response.signedURL();
        } catch (RestClientException ex) {
            throw new StorageException("Impossible de générer l'URL du fichier " + objectKey, ex);
        }
    }

    @Override
    public Map<String, String> presignedDownloadUrls(List<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return Map.of();
        }
        try {
            List<BatchSignedUrlResponse> responses = client.post()
                    .uri("/object/sign/{bucket}", properties.bucket())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", properties.signedUrlTtl().toSeconds(), "paths", objectKeys))
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<BatchSignedUrlResponse>>() {});
            if (responses == null) {
                throw new StorageException("Réponse de signature multiple vide.", null);
            }
            return responses.stream()
                    .filter(response -> response.path() != null && response.signedURL() != null)
                    .collect(Collectors.toMap(
                            BatchSignedUrlResponse::path,
                            response -> storageBaseUrl + response.signedURL(),
                            (first, ignored) -> first));
        } catch (RestClientException ex) {
            throw new StorageException("Impossible de générer les URLs des images.", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.delete().uri(objectUri("/object", objectKey)).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw new StorageException("Échec de la suppression du fichier " + objectKey, ex);
        }
    }

    private void createPrivateBucket(String bucket) {
        try {
            client.post()
                    .uri("/bucket")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("id", bucket, "name", bucket, "public", false))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Bucket Supabase Storage privé '{}' créé", bucket);
        } catch (RestClientException ex) {
            log.warn("Impossible de créer le bucket '{}' ({}).", bucket, ex.getMessage());
        }
    }

    /** Encode chaque segment de la clé, en conservant les "/" comme séparateurs de dossiers. */
    private Function<UriBuilder, URI> objectUri(String prefix, String objectKey) {
        return uriBuilder -> uriBuilder
                .path(prefix)
                .pathSegment(properties.bucket())
                .pathSegment(objectKey.split("/"))
                .build();
    }

    // Supabase répond 400 avec statusCode "404" dans le corps pour un bucket absent.
    private static boolean isBucketNotFound(HttpClientErrorException ex) {
        return ex.getStatusCode() == HttpStatus.NOT_FOUND
                || ex.getResponseBodyAsString().contains("\"404\"")
                || ex.getResponseBodyAsString().contains("not found");
    }

    private record SignedUrlResponse(String signedURL) {
    }

    private record BatchSignedUrlResponse(String path, String signedURL, String error) {
    }
}
