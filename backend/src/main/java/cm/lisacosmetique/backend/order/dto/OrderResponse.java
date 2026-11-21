package cm.lisacosmetique.backend.order.dto;

import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.entity.OrderItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        long subtotalXaf,
        long deliveryFeeXaf,
        long discountXaf,
        long totalXaf,
        List<OrderItemResponse> items,
        Instant createdAt) {

    public static OrderResponse from(Order order, List<OrderItem> items) {
        List<OrderItemResponse> lines = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProductNameSnapshot(),
                        item.getProductSkuSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                lines,
                order.getCreatedAt());
    }
}
