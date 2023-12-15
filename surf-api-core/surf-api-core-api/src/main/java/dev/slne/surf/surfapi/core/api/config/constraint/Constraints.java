package dev.slne.surf.surfapi.core.api.config.constraint;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.meta.Constraint;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.annotation.*;
import java.lang.reflect.Type;

public final class Constraints {
    private Constraints() {
    }

    public static final class Positive implements Constraint<Number> {
        @Override
        public void validate(@Nullable Number value) throws SerializationException {
            if (value != null && value.doubleValue() < 0) {
                throw new SerializationException(value + " must be positive");
            }
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Min {
        int value();

        final class Factory implements Constraint.Factory<Min, Number> {
            @Override
            public Constraint<Number> make(Min data, Type type) {
                return value -> {
                    if (value != null && value.doubleValue() < data.value()) {
                        throw new SerializationException(value + " must be at least " + data.value());
                    }
                };
            }
        }
    }
}
