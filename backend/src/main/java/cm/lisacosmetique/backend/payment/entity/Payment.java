package cm.lisacosmetique.backend.payment.entity;

import cm.lisacosmetique.backend.common.audit.AuditableEntity;
import cm.lisacosmetique.backend.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(
        name = "uk_payments_reference", columnNames = "payment_reference"))
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "payment_reference", nullable = false, length = 64)
    private String paymentReference;

    /** Code du prestataire (mtn, orange, cash...). */
    @Column(nullable = false, length = 40)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency = "XAF";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "provider_transaction_id", length = 128)
    private String providerTransactionId;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Payment() {
        // requis par JPA
    }

    public Payment(Order order, String paymentReference, String provider,
            PaymentMethod paymentMethod, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant d'un paiement doit être positif.");
        }
        this.order = order;
        this.paymentReference = paymentReference;
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public void markProcessing() {
        if (paymentStatus == PaymentStatus.PENDING) {
            paymentStatus = PaymentStatus.PROCESSING;
        }
    }

    /** Transition vers SUCCESS : seul un paiement en attente peut réussir. */
    public void markSuccess(String transactionId, Instant paidAt) {
        if (paymentStatus == PaymentStatus.SUCCESS) {
            return;
        }
        if (paymentStatus != PaymentStatus.PENDING && paymentStatus != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Un paiement " + paymentStatus + " ne peut pas passer à SUCCESS.");
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Un paiement réussi exige un identifiant de transaction.");
        }
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.providerTransactionId = transactionId;
        this.paidAt = paidAt;
    }

    public void markFailed() {
        if (paymentStatus != PaymentStatus.SUCCESS) {
            paymentStatus = PaymentStatus.FAILED;
        }
    }

    public void markCancelled() {
        if (paymentStatus != PaymentStatus.SUCCESS) {
            paymentStatus = PaymentStatus.CANCELLED;
        }
    }

    public void markRefunded() {
        if (paymentStatus != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Seul un paiement réussi peut être remboursé.");
        }
        paymentStatus = PaymentStatus.REFUNDED;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getProvider() {
        return provider;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
