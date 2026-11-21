package cm.lisacosmetique.backend.catalog.dto;

import cm.lisacosmetique.backend.catalog.entity.ProductImage;
import java.util.UUID;

/** Image de catalogue. L'URL est signée et ne donne pas un accès public au bucket. */
public record ProductImageResponse(
        UUID id,
        String imageUrl,
        String altText,
        int position,
        boolean primary) {

    public static ProductImageResponse from(ProductImage image, String imageUrl) {
        return new ProductImageResponse(
                image.getId(), imageUrl, image.getAltText(), image.getPosition(), image.isPrimary());
    }
}
