package cm.lisacosmetique.backend.cart.dto;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        List<CartItemResponse> items,
        long subtotalXaf,
        int totalQuantity) {
}
