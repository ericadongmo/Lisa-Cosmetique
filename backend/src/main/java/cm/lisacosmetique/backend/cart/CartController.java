package cm.lisacosmetique.backend.cart;

import cm.lisacosmetique.backend.cart.dto.AddCartItemRequest;
import cm.lisacosmetique.backend.cart.dto.CartResponse;
import cm.lisacosmetique.backend.cart.dto.UpdateCartItemRequest;
import cm.lisacosmetique.backend.cart.service.CartService;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.user.security.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panier courant. Un client authentifié utilise son panier utilisateur ; un
 * visiteur est identifié par le jeton opaque X-Cart-Token (stocké en
 * localStorage côté frontend, jamais en clair en base).
 */
@RestController
@RequestMapping("/api/v1/cart")
@Profile("!nodb")
@Tag(name = "Panier", description = "Panier client ou invité")
public class CartController {

    public static final String CART_TOKEN_HEADER = "X-Cart-Token";
    private static final int MAX_TOKEN_LENGTH = 200;

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Affiche le panier courant")
    public ResponseEntity<CartResponse> cart(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String token) {
        ResolvedCart resolved = resolve(token);
        return respond(resolved);
    }

    @PostMapping("/items")
    @Operation(summary = "Ajoute un produit au panier")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody AddCartItemRequest request) {
        ResolvedCart resolved = resolve(token);
        CartResponse cart = cartService.addItem(resolved.cart().id(), request);
        return respond(new ResolvedCart(cart, resolved.guestToken()));
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Modifie la quantité d'une ligne (0 = suppression)")
    public ResponseEntity<CartResponse> updateQuantity(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String token,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        ResolvedCart resolved = resolve(token);
        CartResponse cart = cartService.updateQuantity(resolved.cart().id(), productId,
                request.quantity());
        return respond(new ResolvedCart(cart, resolved.guestToken()));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Retire un produit du panier")
    public ResponseEntity<CartResponse> removeItem(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String token,
            @PathVariable UUID productId) {
        ResolvedCart resolved = resolve(token);
        CartResponse cart = cartService.removeItem(resolved.cart().id(), productId);
        return respond(new ResolvedCart(cart, resolved.guestToken()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Vide le panier")
    public void clear(
            @RequestHeader(value = CART_TOKEN_HEADER, required = false) String token) {
        ResolvedCart resolved = resolve(token);
        cartService.clear(resolved.cart().id());
    }

    private ResolvedCart resolve(String rawToken) {
        var user = currentUserService.currentUser();
        if (user.isPresent()) {
            return new ResolvedCart(cartService.getOrCreateForUser(user.get().getId()), null);
        }
        String token = rawToken == null || rawToken.isBlank()
                ? UUID.randomUUID().toString()
                : rawToken.trim();
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new BusinessException("Jeton de panier invalide.");
        }
        return new ResolvedCart(cartService.getOrCreateForGuest(token), token);
    }

    private static ResponseEntity<CartResponse> respond(ResolvedCart resolved) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (resolved.guestToken() != null) {
            builder.header(CART_TOKEN_HEADER, resolved.guestToken());
        }
        return builder.body(resolved.cart());
    }

    private record ResolvedCart(CartResponse cart, String guestToken) {
    }
}
