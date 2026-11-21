package cm.lisacosmetique.backend.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Notification brute d'un prestataire, conservée pour l'audit et l'idempotence. */
@Entity
@Table(name = "payment_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_events_provider_event",
        columnNames = {"provider", "provider_event_id"}))
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_event_id", nullable = false, length = 128)
    private String providerEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    protected PaymentEvent() {
        // requis par JPA
    }

    public PaymentEvent(Payment payment, String provider, String providerEventId, String payload) {
        this.payment = payment;
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.payload = payload;
    }

    public void markProcessed(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public UUID getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
