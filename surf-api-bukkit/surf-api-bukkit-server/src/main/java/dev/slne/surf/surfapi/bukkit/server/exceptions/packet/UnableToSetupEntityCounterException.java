package dev.slne.surf.surfapi.bukkit.server.exceptions.packet;

import java.io.Serial;

public class UnableToSetupEntityCounterException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -2108340024643572539L;

    public UnableToSetupEntityCounterException(String message, Throwable cause) {
        super(message, cause);
    }
}
