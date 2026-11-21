package cm.lisacosmetique.backend.payment.service;

import cm.lisacosmetique.backend.payment.entity.Payment;

/**
 * Contrat commun aux prestataires Mobile Money. Aucun prestataire n'est encore
 * branché : la phase paiement fournira les implémentations et la vérification
 * serveur des notifications.
 */
public interface PaymentProvider {

    /** Code stocké dans payments.provider (ex. "mtn", "orange"). */
    String code();

    PaymentInitiation initiate(Payment payment);

    /** Vérifie auprès du prestataire qu'une transaction est réellement payée. */
    boolean verifySuccess(String providerTransactionId);

    record PaymentInitiation(String providerReference, String redirectUrl) {
    }
}
