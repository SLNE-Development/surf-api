package dev.slne.surf.surfapi.core.api.config.constraint;

import dev.slne.surf.surfapi.core.api.config.type.DoubleOrDefault;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.meta.Constraint;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.util.OptionalDouble;

public final class Constraints {

  private Constraints() {
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Min {

    double value();

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

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Max {

    double value();

    final class Factory implements Constraint.Factory<Max, Number> {

      @Override
      public Constraint<Number> make(Max data, Type type) {
        return value -> {
          if (value != null && value.doubleValue() > data.value()) {
            throw new SerializationException(value + " must be at most " + data.value());
          }
        };
      }
    }
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Range {

    double min();

    double max();

    final class Factory implements Constraint.Factory<Range, Number> {

      @Override
      public Constraint<Number> make(Range data, Type type) {
        return value -> {
          if (value != null && (value.doubleValue() < data.min()
              || value.doubleValue() > data.max())) {
            throw new SerializationException(
                value + " must be between " + data.min() + " and " + data.max());
          }
        };
      }
    }
  }

  public static final class Positive implements Constraint<Number> {

    @Override
    public void validate(@Nullable Number value) throws SerializationException {
      if (value != null && value.doubleValue() < 0) {
        throw new SerializationException(value + " must be positive");
      }
    }
  }

  public static final class BelowZeroDoubleToDefault implements Constraint<DoubleOrDefault> {

    @Override
    public void validate(final @Nullable DoubleOrDefault container) {
      if (container != null) {
        final OptionalDouble value = container.value();
        if (value.isPresent() && value.getAsDouble() < 0) {
          container.value(OptionalDouble.empty());
        }
      }
    }
  }
}
