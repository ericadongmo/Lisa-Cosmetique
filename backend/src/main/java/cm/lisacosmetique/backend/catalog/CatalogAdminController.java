package cm.lisacosmetique.backend.catalog;

import cm.lisacosmetique.backend.catalog.dto.AdminCategoryResponse;
import cm.lisacosmetique.backend.catalog.dto.AdminProductResponse;
import cm.lisacosmetique.backend.catalog.dto.CategoryRequest;
import cm.lisacosmetique.backend.catalog.dto.PageResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API d'administration du catalogue : /api/v1/admin/** exige le rôle ADMIN. */
@RestController
@RequestMapping("/api/v1/admin")
@Profile("!nodb")
@Tag(name = "Administration — catalogue", description = "CRUD produits et catégories (ADMIN)")
public class CatalogAdminController {

    private final CatalogAdminService catalogAdminService;

    public CatalogAdminController(CatalogAdminService catalogAdminService) {
        this.catalogAdminService = catalogAdminService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Liste toutes les catégories, y compris désactivées")
    public List<AdminCategoryResponse> categories() {
        return catalogAdminService.categories();
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Affiche une catégorie")
    public AdminCategoryResponse category(@PathVariable UUID id) {
        return catalogAdminService.category(id);
    }

    @PostMapping("/categories")
    @Operation(summary = "Crée une catégorie")
    public ResponseEntity<AdminCategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        AdminCategoryResponse created = catalogAdminService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + created.id()))
                .body(created);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Met à jour une catégorie")
    public AdminCategoryResponse updateCategory(@PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return catalogAdminService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une catégorie vide")
    public void deleteCategory(@PathVariable UUID id) {
        catalogAdminService.deleteCategory(id);
    }

    @GetMapping("/products")
    @Operation(summary = "Liste tous les produits, y compris inactifs")
    public PageResponse<AdminProductResponse> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogAdminService.products(q, categoryId, active, page, size);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Affiche un produit")
    public AdminProductResponse product(@PathVariable UUID id) {
        return catalogAdminService.product(id);
    }

    @PostMapping("/products")
    @Operation(summary = "Crée un produit")
    public ResponseEntity<AdminProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        AdminProductResponse created = catalogAdminService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/products/" + created.id()))
                .body(created);
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Met à jour un produit")
    public AdminProductResponse updateProduct(@PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        return catalogAdminService.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Désactive un produit (suppression logique)")
    public void deleteProduct(@PathVariable UUID id) {
        catalogAdminService.deleteProduct(id);
    }
}
