package dev.slne.surf.api.core.server.impl.reflection.reflection;

import java.io.Serial;

public class ProxyCreationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5706973123233542233L;

    public ProxyCreationException(String message) {
        super(message);
    }

    public ProxyCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
