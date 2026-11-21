package cm.lisacosmetique.backend.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.delivery.repository.DeliveryZoneRepository;
import cm.lisacosmetique.backend.support.DatabaseCleaner;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartOrderControllerIntegrationTest {

    private static final String CART_TOKEN_HEADER = "X-Cart-Token";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product product;
    private DeliveryZone zone;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbcTemplate);
        Category category = categoryRepository.save(new Category("Soins", "soins", null, 1));
        product = productRepository.save(new Product(category, "Sérum éclat", "serum-eclat",
                "LC-CART-001", 12500L, 10, "Un sérum", "Description"));
        zone = deliveryZoneRepository.save(new DeliveryZone("Yaoundé centre", "Yaoundé", 1500L, 1));
    }

    @Test
    void parcoursInvitePanierPuisCommande() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CART_TOKEN_HEADER))
                .andExpect(jsonPath("$.totalQuantity").value(0))
                .andReturn();
        String token = first.getResponse().getHeader(CART_TOKEN_HEADER);
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(CART_TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":2}
                                """.formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.subtotalXaf").value(25000))
                .andExpect(jsonPath("$.items[0].productSlug").value("serum-eclat"));

        mockMvc.perform(get("/api/v1/cart").header(CART_TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2));

        mockMvc.perform(get("/api/v1/cart")
                        .header(CART_TOKEN_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(0));

        mockMvc.perform(patch("/api/v1/cart/items/" + product.getId())
                        .header(CART_TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalXaf").value(37500));

        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").doesNotExist())
                .andExpect(jsonPath("$.subtotalXaf").value(37500))
                .andExpect(jsonPath("$.deliveryFeeXaf").value(1500))
                .andExpect(jsonPath("$.totalXaf").value(39000))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andReturn();
        String orderNumber = com.jayway.jsonpath.JsonPath.read(
                orderResult.getResponse().getContentAsString(), "$.orderNumber");

        mockMvc.perform(post("/api/v1/orders/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + orderNumber
                                + "\",\"phone\":\"+237 6 90 00 00 00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.deliveryZoneName").value("Yaoundé centre"))
                .andExpect(jsonPath("$.deliveryCity").value("Yaoundé"))
                .andExpect(jsonPath("$.totalXaf").value(39000))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.customerPhone").doesNotExist())
                .andExpect(jsonPath("$.deliveryAddress").doesNotExist());

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(7);

        mockMvc.perform(delete("/api/v1/cart").header(CART_TOKEN_HEADER, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cart").header(CART_TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    @Test
    void quantiteSuperieureAuStockRefusee() throws Exception {
        String token = mockMvc.perform(get("/api/v1/cart"))
                .andReturn().getResponse().getHeader(CART_TOKEN_HEADER);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(CART_TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":11}
                                """.formatted(product.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Opération impossible"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Stock insuffisant")));
    }

    @Test
    void commandeRefuseeSiZoneInconnue() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":"%s","quantity":1}],
                                 "deliveryZoneId":"%s",
                                 "customerName":"Awa Ngo","customerPhone":"+237690000000",
                                 "customerEmail":null,
                                 "deliveryAddress":{"recipientName":"Awa Ngo","phoneNumber":"+237690000000",
                                   "city":"Yaoundé","neighborhood":"Bastos","streetAddress":"Rue 1",
                                   "additionalInformation":null},
                                 "notes":null}
                                """.formatted(product.getId(), UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void commandeSansLigneRefusee() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[],"deliveryZoneId":"%s",
                                 "customerName":"Awa Ngo","customerPhone":"+237690000000",
                                 "deliveryAddress":{"recipientName":"Awa Ngo","phoneNumber":"+237690000000",
                                   "city":"Yaoundé","neighborhood":"Bastos","streetAddress":"Rue 1"}}
                                """.formatted(zone.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"));
    }

    @Test
    void suiviRefuseTelephoneDifferentOuNumeroInconnu() throws Exception {
        String orderNumber = createOrder();

        mockMvc.perform(post("/api/v1/orders/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + orderNumber
                                + "\",\"phone\":\"+237 6 91 11 11 11\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Commande introuvable."));

        mockMvc.perform(post("/api/v1/orders/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"LC-00000000-XXXXXX\","
                                + "\"phone\":\"+237 6 90 00 00 00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Commande introuvable."));
    }

    @Test
    void suiviRefuseUneRequeteIncomplete() throws Exception {
        mockMvc.perform(post("/api/v1/orders/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"\",\"phone\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"));
    }

    @Test
    void zonesDeLivraisonPubliques() throws Exception {
        mockMvc.perform(get("/api/v1/delivery-zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(zone.getId().toString()))
                .andExpect(jsonPath("$[0].deliveryFeeXaf").value(1500))
                .andExpect(jsonPath("$[0].city").value("Yaoundé"));
    }

    private String createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(1)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.orderNumber");
    }

    private String orderJson(int quantity) {
        return """
                {"items":[{"productId":"%s","quantity":%d}],
                 "deliveryZoneId":"%s",
                 "customerName":"Awa Ngo","customerPhone":"+237690000000",
                 "customerEmail":"awa@example.com",
                 "deliveryAddress":{"recipientName":"Awa Ngo","phoneNumber":"+237690000000",
                   "city":"Yaoundé","neighborhood":"Bastos","streetAddress":"Rue 1",
                   "additionalInformation":"Immeuble bleu"},
                 "notes":"Appeler avant la livraison"}
                """.formatted(product.getId(), quantity, zone.getId());
    }
}
