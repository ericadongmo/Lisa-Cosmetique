package cm.lisacosmetique.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "Le slug doit être en minuscules, sans accent, séparé par des tirets.")
        String slug,
        @Size(max = 500) String description,
        @Size(max = 512) String imageKey,
        @Min(0) int position,
        Boolean active) {
}
