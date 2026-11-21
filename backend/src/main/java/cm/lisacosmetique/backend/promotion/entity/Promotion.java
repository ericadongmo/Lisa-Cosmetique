package cm.lisacosmetique.backend.promotion.entity;

import cm.lisacosmetique.backend.common.audit.AdminAuditableEntity;
import cm.lisacosmetique.backend.catalog.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "promotions")
public class Promotion extends AdminAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** Pourcentage (0-100) ou montant XAF, selon le type. */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(nullable = false)
    private boolean active = true;

    /** Aucun lien = promotion valable sur toute la boutique. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> products = new HashSet<>();

    protected Promotion() {
        // requis par JPA
    }

    public Promotion(String name, DiscountType discountType, BigDecimal discountValue,
            Instant startDate, Instant endDate, Set<Product> products) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.products = new HashSet<>(products);
    }

    public boolean isActiveAt(Instant instant) {
        return active && !instant.isBefore(startDate) && instant.isBefore(endDate);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Product> getProducts() {
        return Set.copyOf(products);
    }
}
