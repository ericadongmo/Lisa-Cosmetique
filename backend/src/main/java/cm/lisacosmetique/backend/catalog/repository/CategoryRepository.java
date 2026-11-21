package cm.lisacosmetique.backend.catalog.repository;

import cm.lisacosmetique.backend.catalog.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByActiveTrueOrderByPositionAscNameAsc();

    /** Vue administration : inclut les catégories désactivées. */
    List<Category> findAllByOrderByPositionAscNameAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
