package cm.lisacosmetique.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Aucun prix n'est accepté du client : les montants sont toujours relus en base.
 */
public record CreateOrderRequest(
        @NotEmpty @Valid List<OrderLineRequest> items,
        @NotNull UUID deliveryZoneId,
        @NotBlank @Size(max = 160) String customerName,
        @NotBlank @Size(max = 30) String customerPhone,
        @Email @Size(max = 320) String customerEmail,
        @NotNull @Valid DeliveryAddressRequest deliveryAddress,
        @Size(max = 1000) String notes) {

    public record OrderLineRequest(
            @NotNull UUID productId,
            @Positive int quantity) {
    }
}
