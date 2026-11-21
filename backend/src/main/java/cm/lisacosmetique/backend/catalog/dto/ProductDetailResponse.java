package cm.lisacosmetique.backend.catalog.dto;

import java.util.UUID;
import java.util.List;

/** Fiche produit complète, servie par /api/v1/products/{slug}. */
public record ProductDetailResponse(
        UUID id,
        String slug,
        String name,
        String shortDescription,
        String description,
        long priceXaf,
        /** Prix avant remise (prix barré), null si le produit n'est pas en promotion. */
        Long compareAtPriceXaf,
        boolean inStock,
        int stock,
        String categorySlug,
        String categoryName,
        boolean featured,
        List<ProductImageResponse> images) {
}
