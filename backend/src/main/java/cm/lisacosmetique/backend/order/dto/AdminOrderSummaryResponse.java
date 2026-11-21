package cm.lisacosmetique.backend.order.dto;

import cm.lisacosmetique.backend.order.entity.Order;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderSummaryResponse(
        UUID id,
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        String customerName,
        String customerPhone,
        String deliveryCity,
        long totalXaf,
        Instant createdAt) {

    public static AdminOrderSummaryResponse from(Order order) {
        return new AdminOrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryZone().getCity(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
