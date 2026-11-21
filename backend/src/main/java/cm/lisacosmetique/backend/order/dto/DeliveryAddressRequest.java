package cm.lisacosmetique.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Adresse fournie à la commande ; elle est figée en JSON dans la commande. */
public record DeliveryAddressRequest(
        @NotBlank @Size(max = 160) String recipientName,
        @NotBlank @Size(max = 30) String phoneNumber,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 160) String neighborhood,
        @NotBlank @Size(max = 300) String streetAddress,
        @Size(max = 500) String additionalInformation) {
}
