package cm.lisacosmetique.backend.cart.repository;

import cm.lisacosmetique.backend.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartIdOrderByCreatedAtAsc(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
}
