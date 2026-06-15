package com.crm.shared.exception;

/**
 * Levée quand un appel machine-à-machine (n8n → backend) présente un secret partagé
 * absent ou invalide. Mappée en HTTP 401 par {@code GlobalExceptionHandler}.
 *
 * @author Riahi Dorsaf
 */
public class CallbackAuthException extends RuntimeException {
    public CallbackAuthException(String message) {
        super(message);
    }
}
