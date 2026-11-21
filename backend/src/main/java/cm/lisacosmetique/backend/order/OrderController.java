package cm.lisacosmetique.backend.order;

import cm.lisacosmetique.backend.order.dto.CreateOrderRequest;
import cm.lisacosmetique.backend.order.dto.OrderResponse;
import cm.lisacosmetique.backend.order.dto.OrderTrackingRequest;
import cm.lisacosmetique.backend.order.dto.OrderTrackingResponse;
import cm.lisacosmetique.backend.order.service.OrderService;
import cm.lisacosmetique.backend.user.security.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Création de commande : accessible aux invités comme aux comptes connectés. */
@RestController
@RequestMapping("/api/v1/orders")
@Profile("!nodb")
@Tag(name = "Commandes", description = "Création et suivi de commande")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @Operation(summary = "Crée une commande à partir des lignes fournies")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.create(request,
                currentUserService.currentUser().orElse(null));
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/track")
    @Operation(summary = "Suivi public d'une commande par numéro et téléphone")
    public OrderTrackingResponse track(@Valid @RequestBody OrderTrackingRequest request) {
        return orderService.track(request);
    }
}
