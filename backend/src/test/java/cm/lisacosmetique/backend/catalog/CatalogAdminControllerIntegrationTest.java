package cm.lisacosmetique.backend.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.support.DatabaseCleaner;
import cm.lisacosmetique.backend.user.entity.Role;
import cm.lisacosmetique.backend.user.entity.User;
import cm.lisacosmetique.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogAdminControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Category category;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbcTemplate);
        userRepository.save(new User("Admin", "Lisa", ADMIN_EMAIL, null,
                passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));
        userRepository.save(new User("Client", "Test", CLIENT_EMAIL, null,
                passwordEncoder.encode(CLIENT_PASSWORD), Role.CUSTOMER));
        category = categoryRepository.save(new Category("Soins", "soins", "Soins visage", 1));
    }

    @Test
    void accesReserveAuRoleAdmin() throws Exception {
        String body = categoryJson("Nouveautes", "nouveautes");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(CLIENT_EMAIL, CLIENT_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("nouveautes"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'nouveautes')]").exists());
    }

    @Test
    void creationProduitVisibleDansLeCataloguePublic() throws Exception {
        String body = productJson("serum-test", "LC-ADM-001", 12500L);

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("LC-ADM-001"))
                .andExpect(jsonPath("$.priceXaf").value(12500))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.categoryId").value(category.getId().toString()));

        mockMvc.perform(get("/api/v1/products/serum-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceXaf").value(12500))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void slugOuSkuDejaUtiliseRenvoie409() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("serum-test", "LC-ADM-002", 12500L)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("autre-slug", "LC-ADM-002", 9000L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit"));

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("serum-test", "LC-ADM-003", 9000L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflit"));
    }

    @Test
    void miseAJourPuisDesactivationLogiqueDuProduit() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("serum-test", "LC-ADM-004", 12500L)))
                .andExpect(status().isCreated());

        UUID productId = productRepository.findBySlug("serum-test").orElseThrow().getId();

        mockMvc.perform(put("/api/v1/admin/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("serum-test", "LC-ADM-004", 15000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceXaf").value(15000));

        mockMvc.perform(get("/api/v1/products/serum-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceXaf").value(15000));

        mockMvc.perform(delete("/api/v1/admin/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/serum-test"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/admin/products")
                        .param("active", "false")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void categorieNonVideNePeutPasEtreSupprimee() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("serum-test", "LC-ADM-005", 12500L)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/admin/categories/" + category.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isConflict());

        UUID emptyId = categoryRepository.save(
                new Category("Accessoires", "accessoires", null, 2)).getId();
        mockMvc.perform(delete("/api/v1/admin/categories/" + emptyId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());
        assertThat(categoryRepository.findById(emptyId)).isEmpty();
    }

    @Test
    void donneesInvalidesRefusees() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Serum-Invalide", "LC-ADM-006", -1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"));

        mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(ADMIN_EMAIL, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("serum-test", "LC-ADM-007", 12500L)
                                .replace("\"compareAtPrice\":null",
                                        "\"compareAtPrice\":10000")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Opération impossible"));
    }

    private String categoryJson(String name, String slug) {
        return """
                {"name":"%s","slug":"%s","description":"Description",
                 "imageKey":null,"position":3,"active":true}
                """.formatted(name, slug);
    }

    private String productJson(String slug, String sku, long price) {
        return """
                {"categoryId":"%s","name":"Sérum test","slug":"%s","sku":"%s",
                 "shortDescription":"Un sérum","description":"Description longue",
                 "price":%d,"compareAtPrice":null,"stockQuantity":5,
                 "lowStockThreshold":2,"active":true,"featured":false}
                """.formatted(category.getId(), slug, sku, price);
    }

    private static String basicAuth(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
