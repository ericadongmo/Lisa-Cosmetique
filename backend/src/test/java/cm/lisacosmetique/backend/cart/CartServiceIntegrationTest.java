package cm.lisacosmetique.backend.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.lisacosmetique.backend.cart.dto.AddCartItemRequest;
import cm.lisacosmetique.backend.cart.dto.CartResponse;
import cm.lisacosmetique.backend.cart.repository.CartRepository;
import cm.lisacosmetique.backend.cart.service.CartService;
import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product product;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbcTemplate);
        Category category = categoryRepository.save(
                new Category("Soins", "soins", null, 1));
        product = productRepository.save(new Product(category, "Sérum éclat", "serum-eclat",
                "LC-TEST-002", 12500L, 10, null, null));
    }

    @Test
    void ajouteEtFusionneLesLignesDUneMêmeProduit() {
        CartResponse cart = cartService.getOrCreateForGuest("jeton-invite-123");
        cart = cartService.addItem(cart.id(), new AddCartItemRequest(product.getId(), 2));
        cart = cartService.addItem(cart.id(), new AddCartItemRequest(product.getId(), 3));

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(5);
        assertThat(cart.subtotalXaf()).isEqualTo(62500L);
        assertThat(cart.totalQuantity()).isEqualTo(5);
    }

    @Test
    void stockDepasseRefuseLAjout() {
        CartResponse cart = cartService.getOrCreateForGuest("jeton-invite-456");

        assertThatThrownBy(() ->
                cartService.addItem(cart.id(), new AddCartItemRequest(product.getId(), 11)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    void quantiteZeroSupprimeLaLigne() {
        CartResponse cart = cartService.getOrCreateForGuest("jeton-invite-789");
        cart = cartService.addItem(cart.id(), new AddCartItemRequest(product.getId(), 2));

        cart = cartService.updateQuantity(cart.id(), product.getId(), 0);

        assertThat(cart.items()).isEmpty();
        assertThat(cart.subtotalXaf()).isZero();
    }

    @Test
    void seulLeHashDuJetonEstStocke() {
        CartResponse cart = cartService.getOrCreateForGuest("jeton-secret-brut");

        assertThat(cartRepository.findBySessionTokenHash("jeton-secret-brut")).isEmpty();
        assertThat(cartRepository.findById(cart.id()).orElseThrow().getSessionTokenHash())
                .isNotNull()
                .hasSize(64)
                .isNotEqualTo("jeton-secret-brut");
    }
}
