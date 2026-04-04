package dev.slne.surf.api.core.server.impl.reflection.reflection;

import java.io.Serial;

public class ProxyInvocationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3335067571221950470L;

    public ProxyInvocationException(String message) {
        super(message);
    }

    public ProxyInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}