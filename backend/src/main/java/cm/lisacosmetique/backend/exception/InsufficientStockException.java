package cm.lisacosmetique.backend.exception;

/** Stock insuffisant au moment de réserver les articles d'une commande. */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
