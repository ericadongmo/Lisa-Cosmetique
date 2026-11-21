package cm.lisacosmetique.backend.catalog.entity;

import cm.lisacosmetique.backend.common.audit.AdminAuditableEntity;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
})
public class Product extends AdminAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String slug;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @NotBlank
    @Column(nullable = false, length = 64)
    private String sku;

    /** Prix en francs CFA : entier, jamais de décimale. */
    @PositiveOrZero
    @Column(nullable = false)
    private long price;

    /** Prix barré éventuel, toujours supérieur au prix courant. */
    @Column(name = "compare_at_price")
    private Long compareAtPrice;

    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Min(0)
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 3;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean featured;

    /** Verrou optimiste pour les modifications concurrentes du catalogue. */
    @Version
    @Column(nullable = false)
    private long version;

    protected Product() {
        // requis par JPA
    }

    public Product(Category category, String name, String slug, String sku, long price,
            int stockQuantity, String shortDescription, String description) {
        if (price < 0) {
            throw new IllegalArgumentException("Le prix ne peut pas être négatif.");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif.");
        }
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.shortDescription = shortDescription;
        this.description = description;
    }

    public void decrementStock(int quantity) {
        if (quantity <= 0 || quantity > stockQuantity) {
            throw new IllegalArgumentException("Quantité de stock invalide.");
        }
        this.stockQuantity -= quantity;
    }

    public void incrementStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantité de stock invalide.");
        }
        this.stockQuantity += quantity;
    }

    /** Mise à jour administrative complète, avec vérification des invariants. */
    public void update(Category category, String name, String slug, String sku,
            String shortDescription, String description, long price, Long compareAtPrice,
            int stockQuantity, int lowStockThreshold, boolean active, boolean featured) {
        if (price < 0) {
            throw new IllegalArgumentException("Le prix ne peut pas être négatif.");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif.");
        }
        if (lowStockThreshold < 0) {
            throw new IllegalArgumentException("Le seuil de stock bas ne peut pas être négatif.");
        }
        if (compareAtPrice != null && compareAtPrice <= price) {
            throw new IllegalArgumentException(
                    "Le prix barré doit être supérieur au prix courant.");
        }
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.shortDescription = shortDescription;
        this.description = description;
        this.price = price;
        this.compareAtPrice = compareAtPrice;
        this.stockQuantity = stockQuantity;
        this.lowStockThreshold = lowStockThreshold;
        this.active = active;
        this.featured = featured;
    }

    public void setCompareAtPrice(Long compareAtPrice) {
        this.compareAtPrice = compareAtPrice;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold;
    }

    public UUID getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public String getSku() {
        return sku;
    }

    public long getPrice() {
        return price;
    }

    public Long getCompareAtPrice() {
        return compareAtPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFeatured() {
        return featured;
    }

    public long getVersion() {
        return version;
    }
}
