package cm.lisacosmetique.backend.payment.service;

import cm.lisacosmetique.backend.payment.entity.Payment;
import cm.lisacosmetique.backend.payment.entity.PaymentStatus;
import java.util.UUID;

/** Vue immuable du paiement, sans entité détachée. duplicate = rejeu détecté. */
public record PaymentResult(
        UUID paymentId,
        String paymentReference,
        PaymentStatus status,
        String providerTransactionId,
        boolean duplicate) {

    public static PaymentResult of(Payment payment, boolean duplicate) {
        return new PaymentResult(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getPaymentStatus(),
                payment.getProviderTransactionId(),
                duplicate);
    }
}
