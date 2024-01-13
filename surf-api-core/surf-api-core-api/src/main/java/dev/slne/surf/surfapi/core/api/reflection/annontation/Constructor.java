package dev.slne.surf.surfapi.core.api.reflection.annontation;

import java.lang.annotation.*;

/**
 * Marks a method in a {@link SurfProxy} as a constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Constructor {
}
