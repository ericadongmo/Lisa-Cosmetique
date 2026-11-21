package cm.lisacosmetique.backend.catalog.repository;

import cm.lisacosmetique.backend.catalog.entity.ProductImage;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByPositionAsc(UUID productId);

    Optional<ProductImage> findFirstByProductIdAndPrimaryTrue(UUID productId);

    List<ProductImage> findByProductIdInAndPrimaryTrue(Collection<UUID> productIds);

    Optional<ProductImage> findByIdAndProductId(UUID id, UUID productId);

    boolean existsByProductIdAndPrimaryTrue(UUID productId);
}
