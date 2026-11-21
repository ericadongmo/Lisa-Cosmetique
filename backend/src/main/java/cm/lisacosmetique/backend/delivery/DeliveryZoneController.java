package cm.lisacosmetique.backend.delivery;

import cm.lisacosmetique.backend.delivery.dto.DeliveryZoneResponse;
import cm.lisacosmetique.backend.delivery.service.DeliveryZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-zones")
@Profile("!nodb")
@Tag(name = "Livraison", description = "Zones et frais de livraison")
public class DeliveryZoneController {

    private final DeliveryZoneService deliveryZoneService;

    public DeliveryZoneController(DeliveryZoneService deliveryZoneService) {
        this.deliveryZoneService = deliveryZoneService;
    }

    @GetMapping
    @Operation(summary = "Liste les zones de livraison actives")
    public List<DeliveryZoneResponse> zones() {
        return deliveryZoneService.activeZones().stream()
                .map(DeliveryZoneResponse::from)
                .toList();
    }
}
