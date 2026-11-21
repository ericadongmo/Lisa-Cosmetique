package cm.lisacosmetique.backend.order.service;

import cm.lisacosmetique.backend.catalog.entity.Product;
import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.delivery.service.DeliveryZoneService;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.InsufficientStockException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import cm.lisacosmetique.backend.order.dto.CreateOrderRequest;
import cm.lisacosmetique.backend.order.dto.OrderResponse;
import cm.lisacosmetique.backend.order.dto.OrderTrackingRequest;
import cm.lisacosmetique.backend.order.dto.OrderTrackingResponse;
import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.entity.OrderItem;
import cm.lisacosmetique.backend.order.entity.OrderStatus;
import cm.lisacosmetique.backend.order.repository.OrderItemRepository;
import cm.lisacosmetique.backend.order.repository.OrderRepository;
import cm.lisacosmetique.backend.payment.entity.PaymentStatus;
import cm.lisacosmetique.backend.promotion.service.PromotionService;
import cm.lisacosmetique.backend.promotion.service.PromotionService.AppliedDiscount;
import cm.lisacosmetique.backend.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!nodb")
public class OrderService {

    private static final String ORDER_NUMBER_PREFIX = "LC-";
    private static final char[] NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final DeliveryZoneService deliveryZoneService;
    private final PromotionService promotionService;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            InventoryService inventoryService,
            DeliveryZoneService deliveryZoneService,
            PromotionService promotionService,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
        this.deliveryZoneService = deliveryZoneService;
        this.promotionService = promotionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Création transactionnelle : verrouillage du stock, validation, calcul des
     * montants, enregistrement de la commande puis décrément du stock. Toute
     * erreur annule l'ensemble.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        return create(request, null);
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, User user) {
        Map<UUID, Integer> quantities = collectQuantities(request);
        List<Product> products = inventoryService.lockProducts(quantities.keySet());
        if (products.size() != quantities.size()) {
            throw new BusinessException("Un des produits de la commande n'existe plus.");
        }

        Map<UUID, Long> lineTotals = new LinkedHashMap<>();
        long subtotal = 0L;
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            if (!product.isActive()) {
                throw new BusinessException("Le produit « " + product.getName() + " » n'est plus disponible.");
            }
            if (quantity > product.getStockQuantity()) {
                throw new InsufficientStockException(
                        "Stock insuffisant pour « " + product.getName() + " » (disponible : "
                                + product.getStockQuantity() + ", demandé : " + quantity + ").");
            }
            long lineTotal = Math.multiplyExact(product.getPrice(), quantity);
            lineTotals.put(product.getId(), lineTotal);
            subtotal = Math.addExact(subtotal, lineTotal);
        }

        DeliveryZone zone = deliveryZoneService.requireActiveZone(request.deliveryZoneId());
        AppliedDiscount discount = promotionService.bestDiscount(lineTotals, Instant.now());

        // Le mode de paiement sera choisi lors de l'initialisation du paiement.
        Order order = new Order(
                nextOrderNumber(),
                user,
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                serializeAddress(request),
                zone,
                subtotal,
                zone.getDeliveryFee(),
                discount.amountXaf(),
                null,
                discount.promotion());
        order.setNotes(request.notes());
        orderRepository.save(order);

        List<OrderItem> items = new ArrayList<>();
        for (Product product : products) {
            items.add(new OrderItem(
                    order,
                    product,
                    product.getName(),
                    product.getSku(),
                    product.getPrice(),
                    quantities.get(product.getId())));
        }
        orderItemRepository.saveAll(items);

        for (Product product : products) {
            inventoryService.decrement(product, quantities.get(product.getId()));
        }

        return OrderResponse.from(order, items);
    }

    /**
     * Annulation : seules les commandes non expédiées et non payées peuvent être
     * annulées. L'appel est idempotent et le stock n'est restitué qu'une fois.
     */
    @Transactional
    public OrderResponse cancel(UUID orderId) {
        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderId));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order, items);
        }
        if (order.getOrderStatus() != OrderStatus.PENDING
                && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(
                    "Une commande " + order.getOrderStatus() + " ne peut plus être annulée.");
        }
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException(
                    "La commande est déjà payée : un remboursement doit être traité avant l'annulation.");
        }

        Map<UUID, Integer> toRestore = new HashMap<>();
        for (OrderItem item : items) {
            if (item.getProduct() != null) {
                toRestore.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
            }
        }
        if (!toRestore.isEmpty()) {
            List<Product> products = inventoryService.lockProducts(toRestore.keySet());
            Map<UUID, Product> byId = new HashMap<>();
            products.forEach(product -> byId.put(product.getId(), product));
            toRestore.forEach((productId, quantity) -> {
                Product product = byId.get(productId);
                if (product != null) {
                    inventoryService.restore(product, quantity);
                }
            });
        }

        order.markCancelled();
        if (order.getPaymentStatus() == PaymentStatus.PENDING
                || order.getPaymentStatus() == PaymentStatus.PROCESSING) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }
        return OrderResponse.from(order, items);
    }

    /**
     * Suivi public : le numéro de commande et le téléphone doivent correspondre.
     * Toute erreur renvoie le même 404 pour ne pas révéler l'existence d'une commande.
     */
    @Transactional(readOnly = true)
    public OrderTrackingResponse track(OrderTrackingRequest request) {
        String phone = normalizePhone(request.phone());
        if (phone.length() < 8) {
            throw new ResourceNotFoundException("Commande introuvable.");
        }

        Order order = orderRepository
                .findByOrderNumber(request.orderNumber().trim().toUpperCase(Locale.ROOT))
                .filter(candidate -> phone.equals(normalizePhone(candidate.getCustomerPhone())))
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable."));

        return OrderTrackingResponse.from(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional(readOnly = true)
    public OrderResponse find(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderId));
        return OrderResponse.from(order, orderItemRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> history(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> OrderResponse.from(order,
                        orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    private static Map<UUID, Integer> collectQuantities(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("Une commande doit contenir au moins un produit.");
        }
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CreateOrderRequest.OrderLineRequest line : request.items()) {
            if (line.quantity() <= 0) {
                throw new BusinessException("Chaque quantité commandée doit être strictement positive.");
            }
            if (quantities.putIfAbsent(line.productId(), line.quantity()) != null) {
                throw new BusinessException(
                        "Un produit ne peut apparaître qu'une fois dans la commande.");
            }
        }
        return quantities;
    }

    private String serializeAddress(CreateOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(request.deliveryAddress());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("L'adresse de livraison est invalide.");
        }
    }

    private String nextOrderNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = ORDER_NUMBER_PREFIX + date + "-" + randomCode();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("Impossible de générer un numéro de commande unique.");
    }

    /** Compare les numéros sans indicatif ni séparateurs : +237 690… et 690… sont équivalents. */
    private static String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.startsWith("00237")) {
            digits = digits.substring(2);
        }
        if (digits.startsWith("237") && digits.length() > 9) {
            digits = digits.substring(3);
        }
        return digits.length() > 9 ? digits.substring(digits.length() - 9) : digits;
    }

    private static String randomCode() {
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            builder.append(NUMBER_ALPHABET[RANDOM.nextInt(NUMBER_ALPHABET.length)]);
        }
        return builder.toString();
    }
}
