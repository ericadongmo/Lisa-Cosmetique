package cm.lisacosmetique.backend.delivery.service;

import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.delivery.repository.DeliveryZoneRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!nodb")
@Transactional(readOnly = true)
public class DeliveryZoneService {

    private final DeliveryZoneRepository deliveryZoneRepository;

    public DeliveryZoneService(DeliveryZoneRepository deliveryZoneRepository) {
        this.deliveryZoneRepository = deliveryZoneRepository;
    }

    public List<DeliveryZone> activeZones() {
        return deliveryZoneRepository.findAllByActiveTrueOrderByCityAscNameAsc();
    }

    /** Les frais viennent toujours de la zone en base, jamais du client. */
    public DeliveryZone requireActiveZone(UUID zoneId) {
        DeliveryZone zone = deliveryZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Zone de livraison introuvable : " + zoneId));
        if (!zone.isActive()) {
            throw new BusinessException("Cette zone de livraison n'est pas disponible.");
        }
        return zone;
    }
}
