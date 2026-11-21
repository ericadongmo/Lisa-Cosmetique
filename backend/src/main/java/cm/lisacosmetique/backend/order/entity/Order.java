package cm.lisacosmetique.backend.order.entity;

import cm.lisacosmetique.backend.common.audit.AuditableEntity;
import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.payment.entity.PaymentMethod;
import cm.lisacosmetique.backend.payment.entity.PaymentStatus;
import cm.lisacosmetique.backend.promotion.entity.Promotion;
import cm.lisacosmetique.backend.user.entity.User;
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
import jakarta.persistence.Version;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_number", columnNames = "order_number"))
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 32)
    private String orderNumber;

    /** Nul pour une commande invitée ou si le compte a été supprimé. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "customer_name", nullable = false, length = 160)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "customer_email", length = 320)
    private String customerEmail;

    /** Instantané JSON de l'adresse au moment de la commande. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address_snapshot", nullable = false)
    private String deliveryAddressSnapshot;

    /** Zone utilisée pour calculer les frais : conserve la traçabilité du tarif. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_zone_id", nullable = false)
    private DeliveryZone deliveryZone;

    @Column(nullable = false)
    private long subtotal;

    @Column(name = "delivery_fee", nullable = false)
    private long deliveryFee;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /** Nul tant qu'aucun paiement n'a été initié : le mode est porté par Payment. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(length = 1000)
    private String notes;

    /** Promotion appliquée (facultatif) pour la traçabilité de la remise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Version
    @Column(nullable = false)
    private long version;

    protected Order() {
        // requis par JPA
    }

    public Order(String orderNumber, User user, String customerName, String customerPhone,
            String customerEmail, String deliveryAddressSnapshot, DeliveryZone deliveryZone,
            long subtotal, long deliveryFee, long discountAmount, PaymentMethod paymentMethod,
            Promotion promotion) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.deliveryAddressSnapshot = deliveryAddressSnapshot;
        this.deliveryZone = deliveryZone;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.discountAmount = discountAmount;
        this.totalAmount = subtotal + deliveryFee - discountAmount;
        if (totalAmount < 0) {
            throw new IllegalArgumentException("Le total d'une commande ne peut pas être négatif.");
        }
        this.paymentMethod = paymentMethod;
        this.promotion = promotion;
    }

    public void markCancelled() {
        this.orderStatus = OrderStatus.CANCELLED;
    }

    /**
     * Transitions manuelles autorisées. L'annulation passe par le service
     * dédié car elle doit restituer le stock.
     */
    public void transitionTo(OrderStatus target) {
        boolean allowed = switch (orderStatus) {
            case PENDING -> target == OrderStatus.CONFIRMED;
            case CONFIRMED -> target == OrderStatus.PROCESSING;
            case PROCESSING -> target == OrderStatus.SHIPPED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException(
                    "Transition de statut impossible : " + orderStatus + " vers " + target + ".");
        }
        this.orderStatus = target;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public User getUser() {
        return user;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getDeliveryAddressSnapshot() {
        return deliveryAddressSnapshot;
    }

    public DeliveryZone getDeliveryZone() {
        return deliveryZone;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getDeliveryFee() {
        return deliveryFee;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public Promotion getPromotion() {
        return promotion;
    }
}
