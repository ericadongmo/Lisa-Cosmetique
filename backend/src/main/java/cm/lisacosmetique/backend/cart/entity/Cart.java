package cm.lisacosmetique.backend.cart.entity;

import cm.lisacosmetique.backend.common.audit.AuditableEntity;
import cm.lisacosmetique.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Empreinte SHA-256 du jeton invité : la valeur brute n'est jamais stockée. */
    @Column(name = "session_token_hash", length = 64)
    private String sessionTokenHash;

    protected Cart() {
        // requis par JPA
    }

    public Cart(User user, String sessionTokenHash) {
        this.user = user;
        this.sessionTokenHash = sessionTokenHash;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSessionTokenHash() {
        return sessionTokenHash;
    }
}
