package cm.lisacosmetique.backend.delivery.dto;

import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import java.util.UUID;

public record DeliveryZoneResponse(
        UUID id,
        String name,
        String city,
        long deliveryFeeXaf,
        int estimatedDeliveryDays) {

    public static DeliveryZoneResponse from(DeliveryZone zone) {
        return new DeliveryZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getCity(),
                zone.getDeliveryFee(),
                zone.getEstimatedDeliveryDays());
    }
}
