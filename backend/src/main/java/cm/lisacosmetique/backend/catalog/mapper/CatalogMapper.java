package cm.lisacosmetique.backend.catalog.mapper;

import cm.lisacosmetique.backend.catalog.dto.CategoryResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductDetailResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductImageResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductSummaryResponse;
import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.entity.ProductImage;
import cm.lisacosmetique.backend.catalog.repository.ProductImageRepository;
import cm.lisacosmetique.backend.storage.StorageException;
import cm.lisacosmetique.backend.storage.StorageService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Convertit les entités du catalogue en DTO : aucune entité JPA ne sort de l'API. */
@Component
public class CatalogMapper {

    private static final Logger log = LoggerFactory.getLogger(CatalogMapper.class);

    private final ObjectProvider<ProductImageRepository> imageRepositoryProvider;
    private final ObjectProvider<StorageService> storageProvider;

    public CatalogMapper(
            ObjectProvider<ProductImageRepository> imageRepositoryProvider,
            ObjectProvider<StorageService> storageProvider) {
        this.imageRepositoryProvider = imageRepositoryProvider;
        this.storageProvider = storageProvider;
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                category.getDescription());
    }

    public ProductSummaryResponse toSummary(Product product) {
        return toSummaries(List.of(product)).getFirst();
    }

    public List<ProductSummaryResponse> toSummaries(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        ProductImageRepository imageRepository = imageRepositoryProvider.getIfAvailable();
        StorageService storage = storageProvider.getIfAvailable();
        List<UUID> ids = products.stream().map(Product::getId).toList();
        Map<UUID, ProductImage> primaryImages = imageRepository == null
                ? Map.of()
                : imageRepository.findByProductIdInAndPrimaryTrue(ids).stream()
                        .collect(Collectors.toMap(image -> image.getProduct().getId(), Function.identity()));
        Map<String, String> urls = signedUrls(storage, primaryImages.values().stream()
                .map(ProductImage::getObjectKey).toList());
        return products.stream().map(product -> {
            ProductImage image = primaryImages.get(product.getId());
            return summary(product, image == null ? null : urls.get(image.getObjectKey()));
        }).toList();
    }

    private ProductSummaryResponse summary(Product product, String imageUrl) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getShortDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getStockQuantity() > 0,
                product.getCategory().getSlug(),
                product.getCategory().getName(),
                product.isFeatured(),
                imageUrl);
    }

    public ProductDetailResponse toDetail(Product product) {
        ProductImageRepository imageRepository = imageRepositoryProvider.getIfAvailable();
        StorageService storage = storageProvider.getIfAvailable();
        List<ProductImage> productImages = imageRepository == null
                ? List.of()
                : imageRepository.findByProductIdOrderByPositionAsc(product.getId());
        Map<String, String> urls = signedUrls(storage,
                productImages.stream().map(ProductImage::getObjectKey).toList());
        List<ProductImageResponse> images = productImages.stream()
                .map(image -> ProductImageResponse.from(image, urls.get(image.getObjectKey())))
                .toList();
        return new ProductDetailResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getShortDescription(),
                product.getDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getStockQuantity() > 0,
                product.getStockQuantity(),
                product.getCategory().getSlug(),
                product.getCategory().getName(),
                product.isFeatured(),
                images);
    }

    private Map<String, String> signedUrls(StorageService storage, List<String> objectKeys) {
        if (storage == null || objectKeys.isEmpty()) {
            return Map.of();
        }
        try {
            return storage.presignedDownloadUrls(objectKeys);
        } catch (StorageException ex) {
            log.warn("Impossible de signer les images du catalogue ({}).", ex.getMessage());
            return Map.of();
        }
    }
}
