package cm.lisacosmetique.backend.order;

import cm.lisacosmetique.backend.catalog.dto.PageResponse;
import cm.lisacosmetique.backend.order.dto.AdminOrderResponse;
import cm.lisacosmetique.backend.order.dto.AdminOrderSummaryResponse;
import cm.lisacosmetique.backend.order.dto.UpdateOrderStatusRequest;
import cm.lisacosmetique.backend.order.entity.OrderStatus;
import cm.lisacosmetique.backend.order.service.OrderAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Back-office des commandes : /api/v1/admin/** exige le rôle ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/orders")
@Profile("!nodb")
@Tag(name = "Administration — commandes", description = "Suivi et statuts des commandes (ADMIN)")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping
    @Operation(summary = "Liste les commandes, éventuellement filtrées par statut ou texte")
    public PageResponse<AdminOrderSummaryResponse> orders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderAdminService.orders(status, q, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Affiche une commande avec ses coordonnées et son expédition")
    public AdminOrderResponse order(@PathVariable UUID id) {
        return orderAdminService.order(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Fait avancer ou annule une commande")
    public AdminOrderResponse updateStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderAdminService.updateStatus(id, request);
    }
}
