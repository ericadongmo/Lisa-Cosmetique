package cm.lisacosmetique.backend.order.dto;

import cm.lisacosmetique.backend.delivery.entity.Shipment;
import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.entity.OrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Vue administrateur complète : coordonnées et adresse incluses. */
public record AdminOrderResponse(
        UUID id,
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        String customerName,
        String customerPhone,
        String customerEmail,
        JsonNode deliveryAddress,
        String deliveryZoneName,
        String deliveryCity,
        long subtotalXaf,
        long deliveryFeeXaf,
        long discountXaf,
        long totalXaf,
        String notes,
        List<OrderItemResponse> items,
        AdminShipmentResponse shipment,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminOrderResponse from(Order order, List<OrderItem> items,
            Shipment shipment, ObjectMapper objectMapper) {
        List<OrderItemResponse> lines = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProductNameSnapshot(),
                        item.getProductSkuSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .toList();
        return new AdminOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus().name(),
                order.getPaymentStatus().name(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                readAddress(order.getDeliveryAddressSnapshot(), objectMapper),
                order.getDeliveryZone().getName(),
                order.getDeliveryZone().getCity(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getNotes(),
                lines,
                shipment == null ? null : AdminShipmentResponse.from(shipment),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private static JsonNode readAddress(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
