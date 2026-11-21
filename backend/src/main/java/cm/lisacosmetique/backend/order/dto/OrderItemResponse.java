package cm.lisacosmetique.backend.order.dto;

import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productName,
        String sku,
        long unitPriceXaf,
        int quantity,
        long subtotalXaf) {
}
