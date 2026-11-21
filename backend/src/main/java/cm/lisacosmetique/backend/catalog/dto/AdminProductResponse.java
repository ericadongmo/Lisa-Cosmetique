package cm.lisacosmetique.backend.catalog.dto;

import cm.lisacosmetique.backend.catalog.entity.Product;
import java.time.Instant;
import java.util.UUID;

public record AdminProductResponse(
        UUID id,
        UUID categoryId,
        String categorySlug,
        String categoryName,
        String name,
        String slug,
        String sku,
        String shortDescription,
        String description,
        long priceXaf,
        Long compareAtPriceXaf,
        int stockQuantity,
        int lowStockThreshold,
        boolean active,
        boolean featured,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getSlug(),
                product.getCategory().getName(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getShortDescription(),
                product.getDescription(),
                product.getPrice(),
                product.getCompareAtPrice(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.isActive(),
                product.isFeatured(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
