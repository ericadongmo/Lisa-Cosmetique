package cm.lisacosmetique.backend.payment.repository;

import cm.lisacosmetique.backend.payment.entity.PaymentEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);

    Optional<PaymentEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
