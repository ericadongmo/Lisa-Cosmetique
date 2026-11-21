package cm.lisacosmetique.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.delivery.repository.DeliveryZoneRepository;
import cm.lisacosmetique.backend.support.DatabaseCleaner;
import cm.lisacosmetique.backend.user.entity.Role;
import cm.lisacosmetique.backend.user.entity.User;
import cm.lisacosmetique.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderAdminControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@lisa.cm";
    private static final String ADMIN_PASSWORD = "AdminPass123!";
    private static final String CLIENT_EMAIL = "client@lisa.cm";
    private static final String CLIENT_PASSWORD = "ClientPass123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product product;
    private DeliveryZone zone;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbcTemplate);
        userRepository.save(new User("Admin", "Lisa", ADMIN_EMAIL, null,
                passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));
        userRepository.save(new User("Client", "Test", CLIENT_EMAIL, null,
                passwordEncoder.encode(CLIENT_PASSWORD), Role.CUSTOMER));
        Category category = categoryRepository.save(new Category("Soins", "soins", null, 1));
        product = productRepository.save(new Product(category, "Sérum éclat", "serum-eclat",
                "LC-ADM-ORD-001", 12500L, 10, "Un sérum", "Description"));
        zone = deliveryZoneRepository.save(new DeliveryZone("Yaoundé centre", "Yaoundé", 1500L, 1));
    }

    @Test
    void listeReserveeAuRoleAdmin() throws Exception {
        createOrder();

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(CLIENT_EMAIL, CLIENT_PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].customerPhone").value("+237690000000"))
                .andExpect(jsonPath("$.content[0].deliveryCity").value("Yaoundé"))
                .andExpect(jsonPath("$.content[0].totalXaf").value(26500));
    }

    @Test
    void detailCompletPourAdministrateur() throws Exception {
        OrderFixture fixture = createOrder();

        mockMvc.perform(get("/api/v1/admin/orders/" + fixture.id())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Awa Ngo"))
                .andExpect(jsonPath("$.customerEmail").value("awa@example.com"))
                .andExpect(jsonPath("$.deliveryAddress.neighborhood").value("Bastos"))
                .andExpect(jsonPath("$.deliveryZoneName").value("Yaoundé centre"))
                .andExpect(jsonPath("$.notes").value("Appeler avant la livraison"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].sku").value("LC-ADM-ORD-001"))
                .andExpect(jsonPath("$.shipment").doesNotExist());
    }

    @Test
    void cycleCompletJusquALaLivraison() throws Exception {
        OrderFixture fixture = createOrder();

        patchStatus(fixture.id(), "{\"status\":\"CONFIRMED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));

        patchStatus(fixture.id(), "{\"status\":\"PROCESSING\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PROCESSING"));

        patchStatus(fixture.id(),
                "{\"status\":\"SHIPPED\",\"trackingNumber\":\"TRACK-001\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.shipment.trackingNumber").value("TRACK-001"))
                .andExpect(jsonPath("$.shipment.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shipment.shippedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/orders/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + fixture.orderNumber()
                                + "\",\"phone\":\"+237 6 90 00 00 00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("SHIPPED"));

        patchStatus(fixture.id(), "{\"status\":\"DELIVERED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.shipment.status").value("DELIVERED"))
                .andExpect(jsonPath("$.shipment.deliveredAt").isNotEmpty());

        patchStatus(fixture.id(), "{\"status\":\"DELIVERED\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Opération impossible"));
    }

    @Test
    void transitionsInvalidesRefusees() throws Exception {
        OrderFixture fixture = createOrder();

        patchStatus(fixture.id(), "{\"status\":\"SHIPPED\"}")
                .andExpect(status().isUnprocessableEntity());

        patchStatus(fixture.id(), "{\"status\":\"PENDING\"}")
                .andExpect(status().isUnprocessableEntity());

        patchStatus(fixture.id(), "{\"status\":\"INCONNU\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void annulationRestaureLeStockUneSeuleFois() throws Exception {
        OrderFixture fixture = createOrder();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(8);

        patchStatus(fixture.id(), "{\"status\":\"CANCELLED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.paymentStatus").value("CANCELLED"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(10);

        patchStatus(fixture.id(), "{\"status\":\"CANCELLED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(10);
    }

    @Test
    void rechercheParNumeroEtFiltreStatut() throws Exception {
        OrderFixture fixture = createOrder();

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("q", fixture.orderNumber())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("q", "awa")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "DELIVERED")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private OrderFixture createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":"%s","quantity":2}],
                                 "deliveryZoneId":"%s",
                                 "customerName":"Awa Ngo","customerPhone":"+237690000000",
                                 "customerEmail":"awa@example.com",
                                 "deliveryAddress":{"recipientName":"Awa Ngo","phoneNumber":"+237690000000",
                                   "city":"Yaoundé","neighborhood":"Bastos","streetAddress":"Rue 1",
                                   "additionalInformation":"Immeuble bleu"},
                                 "notes":"Appeler avant la livraison"}
                                """.formatted(product.getId(), zone.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new OrderFixture(
                com.jayway.jsonpath.JsonPath.read(body, "$.id"),
                com.jayway.jsonpath.JsonPath.read(body, "$.orderNumber"));
    }

    private org.springframework.test.web.servlet.ResultActions patchStatus(String id, String body)
            throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/orders/" + id + "/status")
                .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String basicAuth(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private record OrderFixture(String id, String orderNumber) {
    }
}
