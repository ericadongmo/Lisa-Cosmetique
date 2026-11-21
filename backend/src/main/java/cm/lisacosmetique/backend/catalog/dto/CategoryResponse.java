package cm.lisacosmetique.backend.catalog.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String slug, String name, String description) {
}
