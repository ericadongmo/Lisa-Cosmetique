package cm.lisacosmetique.backend.cart.repository;

import cm.lisacosmetique.backend.cart.entity.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserId(UUID userId);

    Optional<Cart> findBySessionTokenHash(String sessionTokenHash);
}
