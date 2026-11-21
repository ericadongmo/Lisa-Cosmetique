package cm.lisacosmetique.backend.exception;

/** Conflit avec une ressource existante (slug, SKU ou e-mail déjà utilisé). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
