package cm.lisacosmetique.backend.order.dto;

import cm.lisacosmetique.backend.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Changement de statut manuel. Le numéro de suivi n'est utile qu'au passage
 * en SHIPPED et reste facultatif (livraison par la boutique).
 */
public record UpdateOrderStatusRequest(
        @NotNull(message = "Le statut est requis.")
        OrderStatus status,

        @Size(max = 64, message = "Le numéro de suivi est limité à 64 caractères.")
        String trackingNumber) {
}
