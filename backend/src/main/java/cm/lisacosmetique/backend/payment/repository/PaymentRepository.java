package cm.lisacosmetique.backend.payment.repository;

import cm.lisacosmetique.backend.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    boolean existsByProviderAndProviderTransactionId(String provider, String providerTransactionId);
}
