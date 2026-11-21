package cm.lisacosmetique.backend.promotion.repository;

import cm.lisacosmetique.backend.promotion.entity.Promotion;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    @EntityGraph(attributePaths = "products")
    @Query("""
            select distinct p from Promotion p
            where p.active = true
              and p.startDate <= :now
              and p.endDate > :now
            """)
    List<Promotion> findActiveAt(@Param("now") Instant now);
}
