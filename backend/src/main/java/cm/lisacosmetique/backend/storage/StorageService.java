package cm.lisacosmetique.backend.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Stockage des fichiers (images produits…). Les fichiers ne sont jamais publics :
 * on les lit via une URL présignée à durée limitée.
 */
public interface StorageService {

    void upload(String objectKey, InputStream content, long size, String contentType);

    String presignedDownloadUrl(String objectKey);

    /** Génère plusieurs URLs dans un appel, pour éviter une requête Storage par carte produit. */
    default Map<String, String> presignedDownloadUrls(List<String> objectKeys) {
        return objectKeys.stream().distinct()
                .collect(java.util.stream.Collectors.toMap(key -> key, this::presignedDownloadUrl));
    }

    void delete(String objectKey);
}
