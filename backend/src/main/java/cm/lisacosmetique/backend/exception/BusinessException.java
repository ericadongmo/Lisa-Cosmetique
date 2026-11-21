package cm.lisacosmetique.backend.exception;

/**
 * Règle métier violée (stock insuffisant, commande non annulable…).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
