package cm.lisacosmetique.backend.order.service;

import cm.lisacosmetique.backend.catalog.dto.PageResponse;
import cm.lisacosmetique.backend.delivery.entity.Shipment;
import cm.lisacosmetique.backend.delivery.entity.ShipmentStatus;
import cm.lisacosmetique.backend.delivery.repository.ShipmentRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.exception.ConflictException;
import cm.lisacosmetique.backend.exception.ResourceNotFoundException;
import cm.lisacosmetique.backend.order.dto.AdminOrderResponse;
import cm.lisacosmetique.backend.order.dto.AdminOrderSummaryResponse;
import cm.lisacosmetique.backend.order.dto.UpdateOrderStatusRequest;
import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.entity.OrderItem;
import cm.lisacosmetique.backend.order.entity.OrderStatus;
import cm.lisacosmetique.backend.order.repository.OrderItemRepository;
import cm.lisacosmetique.backend.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Back-office des commandes. L'autorisation ADMIN est appliquée par Spring
 * Security sur /api/v1/admin/**. Les transitions sont contrôlées ici et dans
 * l'entité, et l'annulation réutilise la logique de restitution du stock.
 */
@Service
@Profile("!nodb")
@Transactional
public class OrderAdminService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderAdminService(OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ShipmentRepository shipmentRepository,
            OrderService orderService,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> orders(OrderStatus status, String query,
            int page, int size) {
        Page<AdminOrderSummaryResponse> result = orderRepository.adminSearch(
                status,
                searchPattern(query),
                PageRequest.of(Math.max(page, 0), clampSize(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(AdminOrderSummaryResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse order(UUID id) {
        return detail(requireOrder(id));
    }

    public AdminOrderResponse updateStatus(UUID id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));

        if (request.status() == OrderStatus.CANCELLED) {
            orderService.cancel(id);
            shipmentRepository.findByOrderId(id).ifPresent(shipment -> {
                if (shipment.getShipmentStatus() != ShipmentStatus.CANCELLED) {
                    shipment.markCancelled();
                }
            });
            return detail(order);
        }

        if (request.status() == OrderStatus.PENDING) {
            throw new BusinessException("Une commande ne peut pas revenir au statut PENDING.");
        }

        try {
            order.transitionTo(request.status());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }

        if (request.status() == OrderStatus.SHIPPED) {
            markShipped(order, request.trackingNumber());
        } else if (request.status() == OrderStatus.DELIVERED) {
            markDelivered(order);
        }

        return detail(order);
    }

    private void markShipped(Order order, String trackingNumber) {
        Shipment shipment = shipmentRepository.findByOrderId(order.getId())
                .orElseGet(() -> shipmentRepository.save(
                        new Shipment(order, order.getDeliveryZone(), null)));
        String normalized = blankToNull(trackingNumber);
        if (normalized != null
                && !normalized.equals(shipment.getTrackingNumber())
                && shipmentRepository.existsByTrackingNumber(normalized)) {
            throw new ConflictException("Ce numéro de suivi est déjà utilisé : " + normalized);
        }
        try {
            shipment.assignTrackingNumber(normalized);
            shipment.markShipped(Instant.now());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }
    }

    private void markDelivered(Order order) {
        Shipment shipment = shipmentRepository.findByOrderId(order.getId())
                .orElseGet(() -> shipmentRepository.save(
                        new Shipment(order, order.getDeliveryZone(), null)));
        try {
            if (shipment.getShipmentStatus() == ShipmentStatus.PENDING) {
                shipment.markShipped(Instant.now());
            }
            shipment.markDelivered(Instant.now());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ex.getMessage());
        }
    }

    private AdminOrderResponse detail(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElse(null);
        return AdminOrderResponse.from(order, items, shipment, objectMapper);
    }

    private Order requireOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + id));
    }

    private static String searchPattern(String query) {
        String value = blankToNull(query);
        return value == null ? null : "%" + value.toLowerCase() + "%";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
