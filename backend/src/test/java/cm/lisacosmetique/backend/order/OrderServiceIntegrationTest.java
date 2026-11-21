package cm.lisacosmetique.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.delivery.repository.DeliveryZoneRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.InsufficientStockException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import cm.lisacosmetique.backend.order.dto.CreateOrderRequest;
import cm.lisacosmetique.backend.order.dto.DeliveryAddressRequest;
import cm.lisacosmetique.backend.order.dto.OrderResponse;
import cm.lisacosmetique.backend.order.dto.OrderTrackingRequest;
import cm.lisacosmetique.backend.order.dto.OrderTrackingResponse;
import cm.lisacosmetique.backend.order.entity.OrderItem;
import cm.lisacosmetique.backend.order.repository.OrderItemRepository;
import cm.lisacosmetique.backend.order.repository.OrderRepository;
import cm.lisacosmetique.backend.order.service.OrderService;
import cm.lisacosmetique.backend.promotion.entity.DiscountType;
import cm.lisacosmetique.backend.promotion.entity.Promotion;
import cm.lisacosmetique.backend.promotion.repository.PromotionRepository;
import cm.lisacosmetique.backend.support.DatabaseCleaner;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tests d'intégration sur H2 : le schéma est généré par Hibernate, pas par Flyway. */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;
    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product product;
    private DeliveryZone zone;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbcTemplate);
        Category category = categoryRepository.save(
                new Category("Soins", "soins", "Soins du visage", 1));
        product = productRepository.save(new Product(category, "Sérum éclat", "serum-eclat",
                "LC-TEST-001", 12500L, 10, "Éclat", "Sérum test"));
        zone = deliveryZoneRepository.save(new DeliveryZone("Yaoundé centre", "Yaoundé", 1500L, 1));
    }

    @Test
    void creationCalculeLesMontantsEtDecrementeLeStock() {
        OrderResponse response = orderService.create(request(2));

        assertThat(response.subtotalXaf()).isEqualTo(25000L);
        assertThat(response.deliveryFeeXaf()).isEqualTo(1500L);
        assertThat(response.discountXaf()).isZero();
        assertThat(response.totalXaf()).isEqualTo(26500L);
        assertThat(response.orderStatus()).isEqualTo("PENDING");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().unitPriceXaf()).isEqualTo(12500L);
        assertThat(response.items().getFirst().subtotalXaf()).isEqualTo(25000L);

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(8);

        List<OrderItem> items = orderItemRepository.findByOrderId(response.id());
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getProductSkuSnapshot()).isEqualTo("LC-TEST-001");
        assertThat(items.getFirst().getProductNameSnapshot()).isEqualTo("Sérum éclat");
    }

    @Test
    void stockInsuffisantRefuseLaCommandeSansModifierLeStock() {
        assertThatThrownBy(() -> orderService.create(request(11)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(10);
    }

    @Test
    void produitInconnuRefuseLaCommande() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderLineRequest(UUID.randomUUID(), 1)),
                zone.getId(), "Awa Ngo", "+237650000000",
                "awa@example.cm", address(), null);

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("n'existe plus");
    }

    @Test
    void meilleurePromotionAppliqueeSansCumul() {
        Instant now = Instant.now();
        promotionRepository.save(new Promotion("Dix pour cent", DiscountType.PERCENTAGE,
                new BigDecimal("10.00"), now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS),
                Set.of(product)));

        OrderResponse response = orderService.create(request(2));

        assertThat(response.discountXaf()).isEqualTo(2500L);
        assertThat(response.totalXaf()).isEqualTo(24000L);
    }

    @Test
    void annulationRestaureLeStockUneSeuleFois() {
        OrderResponse created = orderService.create(request(2));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(8);

        OrderResponse cancelled = orderService.cancel(created.id());
        assertThat(cancelled.orderStatus()).isEqualTo("CANCELLED");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(10);

        OrderResponse again = orderService.cancel(created.id());
        assertThat(again.orderStatus()).isEqualTo("CANCELLED");
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(10);
    }

    @Test
    void suiviRetrouveLaCommandeAvecUnTelephoneFormate() {
        OrderResponse created = orderService.create(request(2));

        OrderTrackingResponse tracked = orderService.track(
                new OrderTrackingRequest(created.orderNumber().toLowerCase(), "+237 6 50 00 00 00"));

        assertThat(tracked.orderNumber()).isEqualTo(created.orderNumber());
        assertThat(tracked.orderStatus()).isEqualTo("PENDING");
        assertThat(tracked.paymentStatus()).isEqualTo("PENDING");
        assertThat(tracked.deliveryZoneName()).isEqualTo("Yaoundé centre");
        assertThat(tracked.deliveryCity()).isEqualTo("Yaoundé");
        assertThat(tracked.totalXaf()).isEqualTo(26500L);
        assertThat(tracked.items()).hasSize(1);
        assertThat(tracked.items().getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void suiviRefuseUnAutreTelephone() {
        OrderResponse created = orderService.create(request(1));

        assertThatThrownBy(() -> orderService.track(
                new OrderTrackingRequest(created.orderNumber(), "+237 6 90 00 00 00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Commande introuvable.");
    }

    private CreateOrderRequest request(int quantity) {
        return new CreateOrderRequest(
                List.of(new CreateOrderRequest.OrderLineRequest(product.getId(), quantity)),
                zone.getId(),
                "Awa Ngo",
                "+237650000000",
                "awa@example.cm",
                address(),
                "Livrer après 17 h");
    }

    private static DeliveryAddressRequest address() {
        return new DeliveryAddressRequest("Awa Ngo", "+237650000000", "Yaoundé",
                "Bastos", "Rue 1.234", "Portail bleu");
    }
}
