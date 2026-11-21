package cm.lisacosmetique.backend.catalog.entity;

import cm.lisacosmetique.backend.common.audit.AdminAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug"))
public class Category extends AdminAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String slug;

    @Column(length = 500)
    private String description;

    /** Clé de l'objet dans Supabase Storage (jamais l'URL publique). */
    @Column(name = "image_key", length = 512)
    private String imageKey;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int position;

    protected Category() {
        // requis par JPA
    }

    public Category(String name, String slug, String description, int position) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.position = position;
    }

    /** Mise à jour administrative : le slug et le nom restent uniques (vérifié par le service). */
    public void update(String name, String slug, String description, String imageKey,
            int position, boolean active) {
        if (position < 0) {
            throw new IllegalArgumentException("La position ne peut pas être négative.");
        }
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageKey = imageKey;
        this.position = position;
        this.active = active;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
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

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getImageKey() {
        return imageKey;
    }

    public boolean isActive() {
        return active;
    }

    public int getPosition() {
        return position;
    }
}
