package dev.slne.surf.surfapi.core.api.reflection.annontation;

import java.lang.annotation.*;

/**
 * Marks a method in a {@link SurfProxy} as a Field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Field {

    String name() default "";

    Type type();

    enum Type {
        SETTER,
        GETTER
    }
}
