package cm.lisacosmetique.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.lisacosmetique.backend.delivery.entity.DeliveryZone;
import cm.lisacosmetique.backend.delivery.repository.DeliveryZoneRepository;
import cm.lisacosmetique.backend.exception.BusinessException;
import cm.lisacosmetique.backend.order.entity.Order;
import cm.lisacosmetique.backend.order.repository.OrderRepository;
import cm.lisacosmetique.backend.payment.entity.Payment;
import cm.lisacosmetique.backend.payment.entity.PaymentMethod;
import cm.lisacosmetique.backend.payment.entity.PaymentStatus;
import cm.lisacosmetique.backend.payment.repository.PaymentEventRepository;
import cm.lisacosmetique.backend.payment.service.PaymentNotification;
import cm.lisacosmetique.backend.payment.service.PaymentResult;
import cm.lisacosmetique.backend.payment.service.PaymentService;
import cm.lisacosmetique.backend.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentEventRepository paymentEventRepository;
    @Autowired
    private DeliveryZoneRepository deliveryZoneRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Payment payment;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.clean(jdbcTemplate);
        DeliveryZone zone = deliveryZoneRepository.save(
                new DeliveryZone("Yaoundé centre", "Yaoundé", 0L, 1));
        Order order = orderRepository.save(new Order(
                "LC-TEST-PAY-1", null, "Awa Ngo", "+237650000000", "awa@example.cm",
                "{\"city\":\"Yaoundé\"}", zone, 10000L, 0L, 0L,
                PaymentMethod.MTN_MOMO, null));
        payment = paymentService.createPayment(order, "MTN", PaymentMethod.MTN_MOMO);
        assertThat(payment.getProvider()).isEqualTo("mtn");
    }

    @Test
    void notificationRejoueeNEstTraiteeQuUneFois() {
        PaymentNotification notification = new PaymentNotification(
                "mtn", "evt-001", payment.getPaymentReference(), "txn-001",
                PaymentStatus.SUCCESS, "{\"status\":\"SUCCESSFUL\"}");

        PaymentResult first = paymentService.handleNotification(notification);
        PaymentResult second = paymentService.handleNotification(notification);

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).isTrue();
        assertThat(paymentEventRepository.count()).isEqualTo(1);
        assertThat(second.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(second.providerTransactionId()).isEqualTo("txn-001");
        assertThat(orderRepository.findById(payment.getOrder().getId()).orElseThrow()
                .getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void succesSansIdentifiantDeTransactionRefuse() {
        PaymentNotification notification = new PaymentNotification(
                "mtn", "evt-002", payment.getPaymentReference(), null,
                PaymentStatus.SUCCESS, "{}");

        assertThatThrownBy(() -> paymentService.handleNotification(notification))
                .isInstanceOf(BusinessException.class);
        assertThat(paymentEventRepository.count()).isZero();
    }

    @Test
    void payloadJsonInvalideRefuse() {
        PaymentNotification notification = new PaymentNotification(
                "mtn", "evt-003", payment.getPaymentReference(), "txn-003",
                PaymentStatus.FAILED, "pas-du-json");

        assertThatThrownBy(() -> paymentService.handleNotification(notification))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JSON");
    }
}
