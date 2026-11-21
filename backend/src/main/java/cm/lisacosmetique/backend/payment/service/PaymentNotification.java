package cm.lisacosmetique.backend.payment.service;

import cm.lisacosmetique.backend.payment.entity.PaymentStatus;

/**
 * Notification normalisée d'un prestataire après vérification de son authenticité.
 * L'identifiant d'événement est la clé d'idempotence.
 */
public record PaymentNotification(
        String provider,
        String eventId,
        String paymentReference,
        String providerTransactionId,
        PaymentStatus status,
        String payload) {
}
