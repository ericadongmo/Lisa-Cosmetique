package cm.lisacosmetique.backend.catalog.dto;

import java.util.UUID;

/** Produit tel qu'affiché dans une liste (catalogue, résultats de recherche). */
public record ProductSummaryResponse(
        UUID id,
        String slug,
        String name,
        String shortDescription,
        long priceXaf,
        /** Prix avant remise (prix barré), null si le produit n'est pas en promotion. */
        Long compareAtPriceXaf,
        boolean inStock,
        String categorySlug,
        String categoryName,
        boolean featured,
        String imageUrl) {
}
