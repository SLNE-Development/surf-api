package dev.slne.surf.api.minestom.server.bootstrap;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import xyz.jpenilla.gremlin.runtime.logging.GremlinLogger;

/**
 * Logs to the console in the layout of the server's default Log4j configuration, which is not on
 * the class path yet while the libraries are being installed.
 *
 * <p>Debug output, e.g. every repository a library is looked up in, is enabled with
 * {@code -Dsurf.minestom.bootstrap.debug=true}.</p>
 */
@NullMarked
final class BootstrapLogger implements GremlinLogger {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final boolean DEBUG = Boolean.getBoolean("surf.minestom.bootstrap.debug");

    @Override
    public void info(final String message, final @Nullable Throwable throwable) {
        this.log(System.out, "INFO", message, throwable);
    }

    @Override
    public void debug(final String message, final @Nullable Throwable throwable) {
        if (DEBUG) {
            this.log(System.out, "DEBUG", message, throwable);
        }
    }

    @Override
    public void warn(final String message, final @Nullable Throwable throwable) {
        this.log(System.err, "WARN", message, throwable);
    }

    @Override
    public void error(final String message, final @Nullable Throwable throwable) {
        this.log(System.err, "ERROR", message, throwable);
    }

    @Override
    public void trace(final String message, final @Nullable Throwable throwable) {
        if (DEBUG) {
            this.log(System.out, "TRACE", message, throwable);
        }
    }

    private void log(final PrintStream stream, final String level, final String message, final @Nullable Throwable throwable) {
        stream.println("[" + TIME.format(LocalTime.now()) + " " + level + "]: [Bootstrap] " + message);
        if (throwable != null) {
            throwable.printStackTrace(stream);
        }
    }
}
