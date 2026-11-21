package cm.lisacosmetique.backend.order.entity;

import cm.lisacosmetique.backend.catalog.entity.Product;
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
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

/**
 * Ligne de commande : les données produit sont figées ici pour préserver
 * l'historique même si le catalogue change ou si le produit est supprimé.
 */
@Entity
@Table(name = "order_items")
public class OrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false, length = 160)
    private String productNameSnapshot;

    @Column(name = "product_sku_snapshot", nullable = false, length = 64)
    private String productSkuSnapshot;

    @PositiveOrZero
    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Positive
    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long subtotal;

    protected OrderItem() {
        // requis par JPA
    }

    public OrderItem(Order order, Product product, String productNameSnapshot,
            String productSkuSnapshot, long unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité commandée doit être strictement positive.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Le prix unitaire ne peut pas être négatif.");
        }
        this.order = order;
        this.product = product;
        this.productNameSnapshot = productNameSnapshot;
        this.productSkuSnapshot = productSkuSnapshot;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice * quantity;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public String getProductSkuSnapshot() {
        return productSkuSnapshot;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getSubtotal() {
        return subtotal;
    }
}
