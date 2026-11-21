package cm.lisacosmetique.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import org.junit.jupiter.api.Test;

class ProductEntityTest {

    private final Category category = new Category("Soins", "soins", null, 1);

    @Test
    void refuseUnPrixNegatif() {
        assertThatThrownBy(() -> new Product(category, "Sérum", "serum", "SKU-1", -1L, 10, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prix");
    }

    @Test
    void refuseUnStockNegatif() {
        assertThatThrownBy(() -> new Product(category, "Sérum", "serum", "SKU-1", 1000L, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock");
    }

    @Test
    void refuseDeDecrémenterPlusQueLeStockDisponible() {
        Product product = new Product(category, "Sérum", "serum", "SKU-1", 1000L, 2, null, null);

        assertThatThrownBy(() -> product.decrementStock(3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(product.getStockQuantity()).isEqualTo(2);
        assertThat(product.isLowStock()).isTrue();
    }

    @Test
    void decrementePuisRestaureLeStock() {
        Product product = new Product(category, "Sérum", "serum", "SKU-1", 1000L, 5, null, null);

        product.decrementStock(2);
        assertThat(product.getStockQuantity()).isEqualTo(3);

        product.incrementStock(4);
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }
}
