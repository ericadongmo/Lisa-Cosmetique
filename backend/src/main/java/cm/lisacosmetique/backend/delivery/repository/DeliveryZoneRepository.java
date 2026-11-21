package cm.lisacosmetique.backend.delivery.repository;

import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {

    List<DeliveryZone> findAllByActiveTrueOrderByCityAscNameAsc();
}
