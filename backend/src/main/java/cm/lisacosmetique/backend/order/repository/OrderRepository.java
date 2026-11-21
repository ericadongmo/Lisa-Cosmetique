package cm.lisacosmetique.backend.order.repository;

import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Recherche administrative : statut et texte libre (numéro, nom, téléphone, e-mail). */
    @Query("""
            select o from Order o
            where (:status is null or o.orderStatus = :status)
              and (:query is null
                or lower(o.orderNumber) like :query
                or lower(o.customerName) like :query
                or lower(o.customerPhone) like :query
                or lower(o.customerEmail) like :query)
            """)
    Page<Order> adminSearch(@Param("status") OrderStatus status,
            @Param("query") String query, Pageable pageable);

    /** Verrou de ligne pour éviter une double annulation / double restauration du stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(@Param("id") UUID id);
}
