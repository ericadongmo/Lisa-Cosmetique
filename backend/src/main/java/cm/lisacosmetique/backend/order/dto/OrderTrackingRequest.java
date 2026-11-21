package cm.lisacosmetique.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Le téléphone sert de secret partagé avec le numéro de commande. */
public record OrderTrackingRequest(
        @NotBlank @Size(max = 32) String orderNumber,
        @NotBlank @Size(max = 30) String phone) {
}
