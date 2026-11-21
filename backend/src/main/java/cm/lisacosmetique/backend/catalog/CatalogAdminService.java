package cm.lisacosmetique.backend.catalog;

import cm.lisacosmetique.backend.catalog.dto.AdminCategoryResponse;
import cm.lisacosmetique.backend.catalog.dto.AdminProductResponse;
import cm.lisacosmetique.backend.catalog.dto.CategoryRequest;
import cm.lisacosmetique.backend.catalog.dto.PageResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductRequest;
import cm.lisacosmetique.backend.catalog.entity.Category;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.ConflictException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration du catalogue. Toute la vérification d'autorisation est faite
 * par Spring Security sur /api/v1/admin/** (rôle ADMIN).
 */
@Service
@Profile("!nodb")
@Transactional
public class CatalogAdminService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CatalogAdminService(CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryResponse> categories() {
        return categoryRepository.findAllByOrderByPositionAscNameAsc().stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCategoryResponse category(UUID id) {
        return AdminCategoryResponse.from(requireCategory(id));
    }

    public AdminCategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Le slug de catégorie est déjà utilisé : " + request.slug());
        }
        Category category = new Category(request.name(), request.slug(),
                request.description(), request.position());
        category.setImageKey(request.imageKey());
        category.setActive(request.active() == null || request.active());
        return AdminCategoryResponse.from(categoryRepository.save(category));
    }

    public AdminCategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = requireCategory(id);
        if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ConflictException("Le slug de catégorie est déjà utilisé : " + request.slug());
        }
        category.update(request.name(), request.slug(), request.description(),
                request.imageKey(), request.position(),
                request.active() == null || request.active());
        return AdminCategoryResponse.from(category);
    }

    /** Suppression réservée aux catégories vides : l'historique produits est protégé. */
    public void deleteCategory(UUID id) {
        Category category = requireCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "Impossible de supprimer une catégorie contenant des produits. "
                            + "Déplacez ou désactivez d'abord ses produits.");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> products(String query, UUID categoryId,
            Boolean active, int page, int size) {
        Page<AdminProductResponse> result = productRepository.adminSearch(
                blankToNull(query), categoryId, active,
                PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("name")))
                .map(AdminProductResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminProductResponse product(UUID id) {
        return AdminProductResponse.from(requireProduct(id));
    }

    public AdminProductResponse createProduct(ProductRequest request) {
        Category category = requireCategory(request.categoryId());
        ensureProductIdentityAvailable(request.slug(), request.sku(), null);
        validatePrices(request);
        Product product = new Product(category, request.name(), request.slug(), request.sku(),
                request.price(), request.stockQuantity(), request.shortDescription(),
                request.description());
        product.setCompareAtPrice(request.compareAtPrice());
        product.setLowStockThreshold(request.lowStockThreshold());
        product.setActive(request.active() == null || request.active());
        product.setFeatured(Boolean.TRUE.equals(request.featured()));
        return AdminProductResponse.from(productRepository.save(product));
    }

    public AdminProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = requireProduct(id);
        Category category = requireCategory(request.categoryId());
        ensureProductIdentityAvailable(request.slug(), request.sku(), id);
        validatePrices(request);
        product.update(category, request.name(), request.slug(), request.sku(),
                request.shortDescription(), request.description(), request.price(),
                request.compareAtPrice(), request.stockQuantity(),
                request.lowStockThreshold(),
                request.active() == null || request.active(),
                Boolean.TRUE.equals(request.featured()));
        return AdminProductResponse.from(product);
    }

    /**
     * Désactivation logique : les commandes passées conservent leur historique.
     * Le produit disparaît du catalogue public mais reste consultable en admin.
     */
    public void deleteProduct(UUID id) {
        requireProduct(id).setActive(false);
    }

    private Category requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + id));
    }

    private void ensureProductIdentityAvailable(String slug, String sku, UUID currentId) {
        boolean slugTaken = currentId == null
                ? productRepository.findBySlug(slug).isPresent()
                : productRepository.existsBySlugAndIdNot(slug, currentId);
        if (slugTaken) {
            throw new ConflictException("Le slug de produit est déjà utilisé : " + slug);
        }
        boolean skuTaken = currentId == null
                ? productRepository.findBySku(sku).isPresent()
                : productRepository.existsBySkuAndIdNot(sku, currentId);
        if (skuTaken) {
            throw new ConflictException("Le SKU est déjà utilisé : " + sku);
        }
    }

    private static void validatePrices(ProductRequest request) {
        if (request.compareAtPrice() != null && request.compareAtPrice() <= request.price()) {
            throw new BusinessException("Le prix barré doit être supérieur au prix courant.");
        }
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
