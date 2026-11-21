package cm.lisacosmetique.backend.cart.dto;

import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String productName,
        String productSlug,
        long unitPriceXaf,
        int quantity,
        long lineTotalXaf,
        int availableStock) {
}
