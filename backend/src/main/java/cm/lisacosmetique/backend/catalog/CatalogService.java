package cm.lisacosmetique.backend.catalog;

import cm.lisacosmetique.backend.catalog.dto.CategoryResponse;
import cm.lisacosmetique.backend.catalog.dto.PageResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductDetailResponse;
import cm.lisacosmetique.backend.catalog.dto.ProductSummaryResponse;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.mapper.CatalogMapper;
import cm.lisacosmetique.backend.catalog.repository.CategoryRepository;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture publique du catalogue : catégories et produits actifs uniquement.
 * Le profil « nodb » démarre sans base : le catalogue n'y est pas disponible.
 */
@Service
@Profile("!nodb")
@Transactional(readOnly = true)
public class CatalogService {

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 48;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogMapper mapper;

    public CatalogService(CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CatalogMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    public List<CategoryResponse> categories() {
        return categoryRepository.findAllByActiveTrueOrderByPositionAscNameAsc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public PageResponse<ProductSummaryResponse> products(
            String category, String query, Boolean featured, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                resolveSort(sort));
        Page<Product> products = productRepository
                .search(blankToNull(category), blankToNull(query), featured, pageable);
        Page<ProductSummaryResponse> result = new PageImpl<>(
                mapper.toSummaries(products.getContent()), products.getPageable(), products.getTotalElements());
        return PageResponse.from(result);
    }

    public ProductDetailResponse product(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug)
                .map(mapper::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + slug));
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /** Le tri accepté est une liste fermée : aucun nom de champ arbitraire n'atteint la base. */
    private static Sort resolveSort(String sort) {
        String value = sort == null ? "" : sort.toLowerCase(Locale.ROOT);
        return switch (value) {
            case "price-asc", "prix-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc", "prix-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest", "nouveautes" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.ASC, "name");
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
