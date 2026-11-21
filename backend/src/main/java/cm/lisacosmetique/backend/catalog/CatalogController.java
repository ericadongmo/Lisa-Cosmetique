package cm.lisacosmetique.backend.catalog;

import cm.lisacosmetique.backend.catalog.dto.CategoryResponse;
import cm.lisacosmetique.backend.catalog.dto.PageResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductDetailResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catalogue public : aucune authentification requise, seuls les produits actifs sont exposés. */
@RestController
@RequestMapping("/api/v1")
@Profile("!nodb")
@Tag(name = "Catalogue", description = "Catégories et produits de la boutique")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Liste les catégories de la boutique")
    public List<CategoryResponse> categories() {
        return catalogService.categories();
    }

    @GetMapping("/products")
    @Operation(summary = "Liste les produits actifs, avec filtres et pagination")
    public PageResponse<ProductSummaryResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sort) {
        return catalogService.products(category, q, featured, page, size, sort);
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Affiche la fiche d'un produit actif")
    public ProductDetailResponse product(@PathVariable String slug) {
        return catalogService.product(slug);
    }
}
