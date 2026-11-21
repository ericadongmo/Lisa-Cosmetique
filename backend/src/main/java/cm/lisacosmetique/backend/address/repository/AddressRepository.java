package cm.lisacosmetique.backend.address.repository;

import cm.lisacosmetique.backend.address.entity.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("update Address a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
