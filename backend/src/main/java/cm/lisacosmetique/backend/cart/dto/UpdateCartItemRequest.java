package cm.lisacosmetique.backend.cart.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Quantité 0 : la ligne est supprimée. */
public record UpdateCartItemRequest(@PositiveOrZero int quantity) {
}
