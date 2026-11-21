package cm.lisacosmetique.backend.catalog.repository;

import cm.lisacosmetique.backend.catalog.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySku(String sku);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    /** Vue administration : produits actifs et inactifs. */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where (cast(:q as string) is null
                   or lower(p.name) like lower(concat('%', cast(:q as string), '%'))
                   or lower(p.sku) like lower(concat('%', cast(:q as string), '%')))
              and (:categoryId is null or p.category.id = :categoryId)
              and (:active is null or p.active = :active)
            """)
    Page<Product> adminSearch(
            @Param("q") String q,
            @Param("categoryId") UUID categoryId,
            @Param("active") Boolean active,
            Pageable pageable);

    /**
     * Recherche publique : uniquement les produits actifs.
     * Les paramètres nuls désactivent le filtre correspondant.
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.active = true
              and (:categorySlug is null or p.category.slug = :categorySlug)
              and (:featured is null or p.featured = :featured)
              and (cast(:q as string) is null
                   or lower(p.name) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(p.shortDescription, '')) like lower(concat('%', cast(:q as string), '%')))
            """)
    Page<Product> search(
            @Param("categorySlug") String categorySlug,
            @Param("q") String q,
            @Param("featured") Boolean featured,
            Pageable pageable);

    /**
     * Verrou pessimiste pour la création de commande : les lignes sont triées
     * par identifiant afin d'éviter les interblocages entre commandes concurrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> lockAllByIdIn(@Param("ids") Collection<UUID> ids);
}
