package cm.lisacosmetique.backend.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Pour les données modifiées depuis l'administration : qui a créé / modifié.
 * L'identité viendra du contexte de sécurité lors de la phase authentification.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AdminAuditableEntity extends AuditableEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }
}
