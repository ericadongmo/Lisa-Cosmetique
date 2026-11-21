package cm.lisacosmetique.backend.order.dto;

import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.entity.OrderItem;
import java.time.Instant;
import java.util.List;

/**
 * Vue publique limitée : aucune adresse, aucun téléphone et aucun e-mail.
 */
public record OrderTrackingResponse(
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        String deliveryZoneName,
        String deliveryCity,
        long totalXaf,
        List<OrderItemResponse> items,
        Instant createdAt) {

    public static OrderTrackingResponse from(Order order, List<OrderItem> items) {
        List<OrderItemResponse> lines = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProductNameSnapshot(),
                        item.getProductSkuSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .toList();
        return new OrderTrackingResponse(
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                order.getDeliveryZone().getName(),
                order.getDeliveryZone().getCity(),
                order.getTotalAmount(),
                lines,
                order.getCreatedAt());
    }
}
