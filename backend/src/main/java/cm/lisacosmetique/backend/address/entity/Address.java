package cm.lisacosmetique.backend.address.entity;

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
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
@Table(name = "addresses")
public class Address extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "recipient_name", nullable = false, length = 160)
    private String recipientName;

    @NotBlank
    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String city;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String neighborhood;

    @NotBlank
    @Column(name = "street_address", nullable = false, length = 300)
    private String streetAddress;

    @Column(name = "additional_information", length = 500)
    private String additionalInformation;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    protected Address() {
        // requis par JPA
    }

    public Address(User user, String recipientName, String phoneNumber, String city,
            String neighborhood, String streetAddress) {
        this.user = user;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.city = city;
        this.neighborhood = neighborhood;
        this.streetAddress = streetAddress;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
