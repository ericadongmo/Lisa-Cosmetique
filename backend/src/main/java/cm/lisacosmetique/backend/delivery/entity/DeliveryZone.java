package cm.lisacosmetique.backend.delivery.entity;

import cm.lisacosmetique.backend.common.audit.AdminAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@Entity
@Table(name = "delivery_zones", uniqueConstraints = @UniqueConstraint(
        name = "uk_delivery_zones_city_name", columnNames = {"city", "name"}))
public class DeliveryZone extends AdminAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String city;

    /** Frais en XAF, toujours calculés côté backend. */
    @PositiveOrZero
    @Column(name = "delivery_fee", nullable = false)
    private long deliveryFee;

    @Min(0)
    @Column(name = "estimated_delivery_days", nullable = false)
    private int estimatedDeliveryDays;

    @Column(nullable = false)
    private boolean active = true;

    protected DeliveryZone() {
        // requis par JPA
    }

    public DeliveryZone(String name, String city, long deliveryFee, int estimatedDeliveryDays) {
        this.name = name;
        this.city = city;
        this.deliveryFee = deliveryFee;
        this.estimatedDeliveryDays = estimatedDeliveryDays;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public long getDeliveryFee() {
        return deliveryFee;
    }

    public int getEstimatedDeliveryDays() {
        return estimatedDeliveryDays;
    }

    public boolean isActive() {
        return active;
    }
}
