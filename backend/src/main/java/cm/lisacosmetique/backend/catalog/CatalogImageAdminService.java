package cm.lisacosmetique.backend.catalog;

import cm.lisacosmetique.backend.catalog.dto.ProductImageResponse;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.entity.ProductImage;
import cm.lisacosmetique.backend.catalog.repository.ProductImageRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import cm.lisacosmetique.backend.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Gestion des images produits. Les fichiers restent privés dans Supabase Storage. */
@Service
@Profile("!nodb")
@Transactional
public class CatalogImageAdminService {

    public static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ObjectProvider<StorageService> storageProvider;

    public CatalogImageAdminService(
            ProductRepository productRepository,
            ProductImageRepository imageRepository,
            ObjectProvider<StorageService> storageProvider) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.storageProvider = storageProvider;
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> images(UUID productId) {
        requireProduct(productId);
        StorageService storage = requireStorage();
        List<ProductImage> images = imageRepository.findByProductIdOrderByPositionAsc(productId);
        var urls = storage.presignedDownloadUrls(
                images.stream().map(ProductImage::getObjectKey).toList());
        return images.stream()
                .map(image -> ProductImageResponse.from(image, urls.get(image.getObjectKey())))
                .toList();
    }

    public ProductImageResponse upload(
            UUID productId, MultipartFile file, String altText, boolean makePrimary) {
        Product product = requireProduct(productId);
        StorageService storage = requireStorage();
        byte[] content = readAndValidate(file);
        ImageFormat format = ImageFormat.detect(content);
        String declaredType = file.getContentType();
        if (declaredType != null && !declaredType.isBlank()
                && !format.contentType().equalsIgnoreCase(declaredType)) {
            throw new BusinessException("Le type du fichier ne correspond pas au contenu de l'image.");
        }

        List<ProductImage> existing = imageRepository.findByProductIdOrderByPositionAsc(productId);
        int position = existing.stream().mapToInt(ProductImage::getPosition).max().orElse(-1) + 1;
        boolean primary = makePrimary || existing.stream().noneMatch(ProductImage::isPrimary);
        if (primary) {
            existing.stream().filter(ProductImage::isPrimary).findFirst().ifPresent(image -> {
                image.setPrimary(false);
                imageRepository.saveAndFlush(image);
            });
        }

        String objectKey = "products/" + productId + "/" + UUID.randomUUID() + format.extension();
        boolean uploaded = false;
        try {
            storage.upload(objectKey, new ByteArrayInputStream(content), content.length, format.contentType());
            uploaded = true;
            ProductImage image = new ProductImage(
                    product, objectKey, normalizeAltText(altText, product.getName()), position, primary);
            ProductImage saved = imageRepository.saveAndFlush(image);
            String imageUrl = storage.presignedDownloadUrl(objectKey);
            return ProductImageResponse.from(saved, imageUrl);
        } catch (RuntimeException ex) {
            if (uploaded) {
                try {
                    storage.delete(objectKey);
                } catch (RuntimeException cleanupError) {
                    ex.addSuppressed(cleanupError);
                }
            }
            throw ex;
        }
    }

    public void delete(UUID productId, UUID imageId) {
        ProductImage image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Image introuvable pour ce produit."));
        StorageService storage = requireStorage();
        storage.delete(image.getObjectKey());
        boolean wasPrimary = image.isPrimary();
        imageRepository.delete(image);
        imageRepository.flush();
        if (wasPrimary) {
            imageRepository.findByProductIdOrderByPositionAsc(productId).stream().findFirst().ifPresent(next -> {
                next.setPrimary(true);
                imageRepository.save(next);
            });
        }
    }

    private Product requireProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + productId));
    }

    private StorageService requireStorage() {
        StorageService storage = storageProvider.getIfAvailable();
        if (storage == null) {
            throw new BusinessException("Le stockage des images n'est pas configuré.");
        }
        return storage;
    }

    private static byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Sélectionnez une image à envoyer.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("L'image ne doit pas dépasser 5 Mo.");
        }
        try {
            byte[] content = file.getBytes();
            ImageFormat.detect(content);
            return content;
        } catch (IOException ex) {
            throw new BusinessException("Impossible de lire le fichier envoyé.");
        }
    }

    private static String normalizeAltText(String altText, String productName) {
        String value = altText == null || altText.isBlank() ? productName : altText.trim();
        return value.length() <= 200 ? value : value.substring(0, 200);
    }

    private enum ImageFormat {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        WEBP("image/webp", ".webp");

        private final String contentType;
        private final String extension;

        ImageFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        String contentType() {
            return contentType;
        }

        String extension() {
            return extension;
        }

        static ImageFormat detect(byte[] bytes) {
            if (bytes != null && bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                    && (bytes[2] & 0xff) == 0xff) {
                return JPEG;
            }
            if (bytes != null && bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                    && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                    && bytes[6] == 0x1a && bytes[7] == 0x0a) {
                return PNG;
            }
            if (bytes != null && bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return WEBP;
            }
            throw new BusinessException("Formats acceptés : JPEG, PNG ou WebP.");
        }
    }
}
