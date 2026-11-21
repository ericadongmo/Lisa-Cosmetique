package cm.lisacosmetique.backend.cart.service;

import cm.lisacosmetique.backend.cart.dto.AddCartItemRequest;
import cm.lisacosmetique.backend.cart.dto.CartItemResponse;
import cm.lisacosmetique.backend.cart.dto.CartResponse;
import cm.lisacosmetique.backend.cart.entity.Cart;
import cm.lisacosmetique.backend.cart.entity.CartItem;
import cm.lisacosmetique.backend.cart.repository.CartItemRepository;
import cm.lisacosmetique.backend.cart.repository.CartRepository;
import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.catalog.repository.ProductRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import cm.lisacosmetique.backend.user.entity.User;
import cm.lisacosmetique.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!nodb")
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getOrCreateForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + userId));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(user, null)));
        return toResponse(cart);
    }

    /**
     * @param rawSessionToken jeton opaque fourni par le client ; seul son
     *                        empreinte SHA-256 est stockée.
     */
    @Transactional
    public CartResponse getOrCreateForGuest(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new BusinessException("Un jeton de session est requis pour un panier invité.");
        }
        String hash = sha256(rawSessionToken.trim());
        Cart cart = cartRepository.findBySessionTokenHash(hash)
                .orElseGet(() -> cartRepository.save(new Cart(null, hash)));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID cartId, AddCartItemRequest request) {
        Cart cart = requireCart(cartId);
        Product product = requireAvailableProduct(request.productId());
        int requested = request.quantity();

        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, product.getId())
                .orElse(null);
        int targetQuantity = item == null ? requested : item.getQuantity() + requested;
        assertStock(product, targetQuantity);

        if (item == null) {
            item = new CartItem(cart, product, targetQuantity);
        } else {
            item.setQuantity(targetQuantity);
        }
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateQuantity(UUID cartId, UUID productId, int quantity) {
        requireCart(cartId);
        if (quantity <= 0) {
            cartItemRepository.findByCartIdAndProductId(cartId, productId)
                    .ifPresent(cartItemRepository::delete);
            return toResponse(cartId);
        }
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ce produit n'est pas dans le panier."));
        assertStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return toResponse(cartId);
    }

    @Transactional
    public CartResponse removeItem(UUID cartId, UUID productId) {
        requireCart(cartId);
        cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .ifPresent(cartItemRepository::delete);
        return toResponse(cartId);
    }

    @Transactional
    public void clear(UUID cartId) {
        requireCart(cartId);
        cartItemRepository.deleteAll(cartItemRepository.findByCartIdOrderByCreatedAtAsc(cartId));
    }

    public CartResponse get(UUID cartId) {
        requireCart(cartId);
        return toResponse(cartId);
    }

    private Cart requireCart(UUID cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Panier introuvable : " + cartId));
    }

    private Product requireAvailableProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + productId));
        if (!product.isActive()) {
            throw new BusinessException("Ce produit n'est plus disponible.");
        }
        return product;
    }

    private static void assertStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BusinessException("Stock insuffisant pour « " + product.getName()
                    + " » (disponible : " + product.getStockQuantity() + ").");
        }
    }

    private CartResponse toResponse(UUID cartId) {
        return toResponse(requireCart(cartId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findByCartIdOrderByCreatedAtAsc(cart.getId()).stream()
                .map(item -> toItemResponse(item))
                .toList();
        long subtotal = items.stream().mapToLong(CartItemResponse::lineTotalXaf).sum();
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(cart.getId(), items, subtotal, totalQuantity);
    }

    private static CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        long lineTotal = Math.multiplyExact(product.getPrice(), item.getQuantity());
        return new CartItemResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                item.getQuantity(),
                lineTotal,
                product.getStockQuantity());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponible sur cette JVM.", ex);
        }
    }
}
