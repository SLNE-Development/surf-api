package dev.slne.surf.redis.codec;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.Decoder;

import java.nio.charset.Charset;
import java.util.UUID;

/**
 * A Redisson codec that handles UUID serialization and deserialization for Redis storage.
 * <p>
 * This codec extends {@link StringCodec} and converts UUIDs to their string representation for storage
 * in Redis, then deserializes them back to UUID objects when retrieved. The UUID is stored as
 * a string using the standard UUID format (e.g., "550e8400-e29b-41d4-a716-446655440000").
 *
 * @see StringCodec
 */
@NullMarked
public class UUIDCodec extends StringCodec {
    public static final UUIDCodec INSTANCE = new UUIDCodec();


    /**
     * Creates a new UUIDCodec with default settings.
     */
    public UUIDCodec() {
    }

    /**
     * Creates a new UUIDCodec with the specified class loader.
     *
     * @param classLoader the class loader to use for deserialization, or null for the default
     */
    public UUIDCodec(@Nullable ClassLoader classLoader) {
        super(classLoader);
    }

    /**
     * Creates a new UUIDCodec with the specified charset.
     *
     * @param charset the charset to use for string encoding
     */
    public UUIDCodec(Charset charset) {
        super(charset);
    }

    /**
     * Creates a new UUIDCodec with the specified charset name.
     *
     * @param charsetName the name of the charset to use for string encoding
     */
    public UUIDCodec(String charsetName) {
        super(charsetName);
    }

    private final Decoder<Object> decoder = (buf, state) -> {
        final String string = (String) UUIDCodec.super.getValueDecoder().decode(buf, state);
        return UUID.fromString(string);
    };

    /**
     * Returns the decoder that converts Redis string values back to UUID objects.
     *
     * @return the value decoder for UUID deserialization
     */
    @Override
    public Decoder<Object> getValueDecoder() {
        return decoder;
    }
}
