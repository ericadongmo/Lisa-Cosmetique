package cm.lisacosmetique.backend.catalog.entity;

import cm.lisacosmetique.backend.common.audit.AuditableEntity;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "product_images", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_images_object_key", columnNames = "object_key"),
        @UniqueConstraint(name = "uk_product_images_position", columnNames = {"product_id", "position"})
})
public class ProductImage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Clé de l'objet dans Supabase Storage, par ex. products/<uuid>.webp. */
    @NotBlank
    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Min(0)
    @Column(nullable = false)
    private int position;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    protected ProductImage() {
        // requis par JPA
    }

    public ProductImage(Product product, String objectKey, String altText, int position, boolean primary) {
        this.product = product;
        this.objectKey = objectKey;
        this.altText = altText;
        this.position = position;
        this.primary = primary;
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getAltText() {
        return altText;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
