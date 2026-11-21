package cm.lisacosmetique.backend.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.mapper.CatalogMapper;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "nodb"})
// Le profil nodb exclut le catalogue (pas de JPA) : on instancie ici ses beans réels.
@Import(CatalogControllerIntegrationTest.CatalogTestConfig.class)
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductRepository productRepository;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = newEntity(Category.class);
        ReflectionTestUtils.setField(category, "id", UUID.fromString("11111111-1111-1111-1111-111111111101"));
        ReflectionTestUtils.setField(category, "slug", "produits-cosmetiques");
        ReflectionTestUtils.setField(category, "name", "Produits cosmétiques");
        ReflectionTestUtils.setField(category, "description", "Visage, maquillage, soins ciblés");
        ReflectionTestUtils.setField(category, "position", 1);

        product = newEntity(Product.class);
        ReflectionTestUtils.setField(product, "id", UUID.fromString("22222222-2222-2222-2222-222222222201"));
        ReflectionTestUtils.setField(product, "category", category);
        ReflectionTestUtils.setField(product, "slug", "serum-eclat-vitamine-c");
        ReflectionTestUtils.setField(product, "name", "Sérum éclat à la vitamine C");
        ReflectionTestUtils.setField(product, "shortDescription", "Un teint lumineux dès quatre semaines");
        ReflectionTestUtils.setField(product, "description", "Sérum concentré à 10 % de vitamine C. Flacon 30 ml.");
        ReflectionTestUtils.setField(product, "price", 12500L);
        ReflectionTestUtils.setField(product, "stockQuantity", 18);
        ReflectionTestUtils.setField(product, "active", true);
        ReflectionTestUtils.setField(product, "featured", true);
        ReflectionTestUtils.setField(product, "createdAt", Instant.parse("2026-11-13T08:00:00Z"));
        ReflectionTestUtils.setField(product, "updatedAt", Instant.parse("2026-11-13T08:00:00Z"));
    }

    @Test
    void categoriesArePublic() throws Exception {
        when(categoryRepository.findAllByActiveTrueOrderByPositionAscNameAsc()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("produits-cosmetiques"))
                .andExpect(jsonPath("$[0].name").value("Produits cosmétiques"));
    }

    @Test
    void productListIsPublicAndPaginated() throws Exception {
        when(productRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/api/v1/products").param("category", "produits-cosmetiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("serum-eclat-vitamine-c"))
                .andExpect(jsonPath("$.content[0].priceXaf").value(12500))
                .andExpect(jsonPath("$.content[0].inStock").value(true))
                .andExpect(jsonPath("$.content[0].categoryName").value("Produits cosmétiques"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void productDetailIsPublic() throws Exception {
        when(productRepository.findBySlugAndActiveTrue("serum-eclat-vitamine-c"))
                .thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/v1/products/serum-eclat-vitamine-c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sérum éclat à la vitamine C"))
                .andExpect(jsonPath("$.description").value("Sérum concentré à 10 % de vitamine C. Flacon 30 ml."))
                .andExpect(jsonPath("$.stock").value(18));
    }

    @Test
    void unknownOrInactiveProductReturnsProblemDetail() throws Exception {
        when(productRepository.findBySlugAndActiveTrue("produit-inconnu")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/produit-inconnu"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CatalogTestConfig {

        @Bean
        CatalogService catalogService(CategoryRepository categoryRepository,
                ProductRepository productRepository,
                CatalogMapper mapper) {
            return new CatalogService(categoryRepository, productRepository, mapper);
        }

        @Bean
        CatalogController catalogController(CatalogService catalogService) {
            return new CatalogController(catalogService);
        }
    }

    private static <T> T newEntity(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Impossible d'instancier " + type.getSimpleName(), ex);
        }
    }
}
