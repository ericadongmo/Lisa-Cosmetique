package cm.lisacosmetique.backend.payment.service;

import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.payment.entity.Payment;
import cm.lisacosmetique.backend.payment.entity.PaymentEvent;
import cm.lisacosmetique.backend.payment.entity.PaymentMethod;
import cm.lisacosmetique.backend.payment.entity.PaymentStatus;
import cm.lisacosmetique.backend.payment.repository.PaymentEventRepository;
import cm.lisacosmetique.backend.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!nodb")
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentEventRepository paymentEventRepository,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Enregistre une tentative de paiement ; le prestataire est appelé en phase paiement. */
    @Transactional
    public Payment createPayment(Order order, String provider, PaymentMethod method) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException("Le prestataire de paiement est requis.");
        }
        String reference = "PAY-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        Payment payment = new Payment(order, reference, provider.trim().toLowerCase(Locale.ROOT),
                method, order.getTotalAmount());
        return paymentRepository.save(payment);
    }

    /**
     * Traite une notification vérifiée. Rejouer le même événement ne réapplique
     * rien : la contrainte UNIQUE(provider, provider_event_id) est le garde-fou
     * final en cas de concurrence.
     */
    @Transactional
    public PaymentResult handleNotification(PaymentNotification notification) {
        validate(notification);
        var existing = paymentEventRepository
                .findByProviderAndProviderEventId(notification.provider(), notification.eventId());
        if (existing.isPresent()) {
            return PaymentResult.of(existing.get().getPayment(), true);
        }

        Payment payment = paymentRepository.findByPaymentReference(notification.paymentReference())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paiement introuvable : " + notification.paymentReference()));
        if (!payment.getProvider().equals(notification.provider())) {
            throw new BusinessException("Le prestataire ne correspond pas au paiement.");
        }
        if (payment.getAmount() != payment.getOrder().getTotalAmount()) {
            throw new BusinessException("Le montant du paiement ne correspond plus à la commande.");
        }

        PaymentEvent event = new PaymentEvent(payment, notification.provider(),
                notification.eventId(), normalizePayload(notification.payload()));

        applyStatus(payment, notification);
        event.markProcessed(Instant.now());
        paymentEventRepository.save(event);
        return PaymentResult.of(payment, false);
    }

    private static void validate(PaymentNotification notification) {
        if (notification.provider() == null || notification.provider().isBlank()
                || notification.eventId() == null || notification.eventId().isBlank()
                || notification.paymentReference() == null || notification.paymentReference().isBlank()) {
            throw new BusinessException("Notification de paiement incomplète.");
        }
        if (notification.status() == null) {
            throw new BusinessException("Statut de paiement manquant.");
        }
    }

    private static void applyStatus(Payment payment, PaymentNotification notification) {
        PaymentStatus before = payment.getPaymentStatus();
        try {
            switch (notification.status()) {
                case SUCCESS -> payment.markSuccess(notification.providerTransactionId(), Instant.now());
                case PROCESSING -> payment.markProcessing();
                case FAILED -> payment.markFailed();
                case CANCELLED -> payment.markCancelled();
                case REFUNDED -> payment.markRefunded();
                case PENDING -> throw new BusinessException(
                        "Une notification ne peut pas ramener un paiement à PENDING.");
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }
        if (payment.getPaymentStatus() != before) {
            payment.getOrder().setPaymentStatus(payment.getPaymentStatus());
        }
    }

    private String normalizePayload(String payload) {
        String value = payload == null || payload.isBlank() ? "{}" : payload;
        try {
            objectMapper.readTree(value);
            return value;
        } catch (JsonProcessingException ex) {
            throw new BusinessException("La charge utile de la notification n'est pas un JSON valide.");
        }
    }
}
