package cm.lisacosmetique.backend.order.dto;

import cm.lisacosmetique.backend.delivery.entity.Shipment;
import java.time.Instant;
import java.util.UUID;

public record AdminShipmentResponse(
        UUID id,
        String trackingNumber,
        String status,
        Instant shippedAt,
        Instant deliveredAt) {

    public static AdminShipmentResponse from(Shipment shipment) {
        return new AdminShipmentResponse(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getShipmentStatus().name(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt());
    }
}
