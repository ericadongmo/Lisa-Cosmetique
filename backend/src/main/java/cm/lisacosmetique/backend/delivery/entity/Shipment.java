package cm.lisacosmetique.backend.delivery.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shipments_order", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_shipments_tracking", columnNames = "tracking_number")
})
public class Shipment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_zone_id", nullable = false)
    private DeliveryZone deliveryZone;

    @Column(name = "tracking_number", length = 64)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false, length = 20)
    private ShipmentStatus shipmentStatus = ShipmentStatus.PENDING;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected Shipment() {
        // requis par JPA
    }

    public Shipment(Order order, DeliveryZone deliveryZone, String trackingNumber) {
        this.order = order;
        this.deliveryZone = deliveryZone;
        this.trackingNumber = trackingNumber;
    }

    /** Renseigne ou corrige le numéro de suivi d'une expédition non terminée. */
    public void assignTrackingNumber(String trackingNumber) {
        if (shipmentStatus == ShipmentStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Le numéro de suivi d'une expédition livrée ne peut plus être modifié.");
        }
        this.trackingNumber = trackingNumber == null || trackingNumber.isBlank()
                ? null
                : trackingNumber.trim();
    }

    public void markShipped(Instant shippedAt) {
        if (shipmentStatus == ShipmentStatus.CANCELLED) {
            throw new IllegalStateException("Une expédition annulée ne peut pas être expédiée.");
        }
        this.shipmentStatus = ShipmentStatus.SHIPPED;
        this.shippedAt = shippedAt;
    }

    public void markDelivered(Instant deliveredAt) {
        if (shipmentStatus != ShipmentStatus.SHIPPED) {
            throw new IllegalStateException("Une expédition doit être expédiée avant d'être livrée.");
        }
        this.shipmentStatus = ShipmentStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void markCancelled() {
        this.shipmentStatus = ShipmentStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public DeliveryZone getDeliveryZone() {
        return deliveryZone;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public ShipmentStatus getShipmentStatus() {
        return shipmentStatus;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }
}
