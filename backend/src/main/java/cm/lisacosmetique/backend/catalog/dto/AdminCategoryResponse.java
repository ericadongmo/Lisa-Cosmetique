package cm.lisacosmetique.backend.catalog.dto;

import cm.lisacosmetique.backend.catalog.entity.Category;
import java.time.Instant;
import java.util.UUID;

public record AdminCategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String imageKey,
        boolean active,
        int position,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminCategoryResponse from(Category category) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageKey(),
                category.isActive(),
                category.getPosition(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
