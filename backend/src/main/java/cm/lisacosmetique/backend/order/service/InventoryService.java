package cm.lisacosmetique.backend.order.service;

import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.exception.InsufficientStockException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Toutes les opérations de stock exigent une transaction appelante : la
 * réservation et la création de commande doivent réussir ou échouer ensemble.
 */
@Service
@Profile("!nodb")
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Verrou pessimiste, trié par identifiant pour prévenir les interblocages. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Product> lockProducts(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return productRepository.lockAllByIdIn(productIds);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void decrement(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive.");
        }
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Stock insuffisant pour « " + product.getName() + " » (disponible : "
                            + product.getStockQuantity() + ", demandé : " + quantity + ").");
        }
        product.decrementStock(quantity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Product product, int quantity) {
        product.incrementStock(quantity);
    }
}
