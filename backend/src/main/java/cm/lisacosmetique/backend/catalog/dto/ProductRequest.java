package cm.lisacosmetique.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Le prix est en XAF entiers ; aucun montant décimal n'est accepté. */
public record ProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 120)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "Le slug doit être en minuscules, sans accent, séparé par des tirets.")
        String slug,
        @NotBlank @Size(max = 64) String sku,
        @Size(max = 300) String shortDescription,
        String description,
        @PositiveOrZero long price,
        Long compareAtPrice,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull @Min(0) Integer lowStockThreshold,
        Boolean active,
        Boolean featured) {
}
