package dev.slne.surf.surfapi.core.api.reflection.annontation;

import java.lang.annotation.*;

/**
 * Overrides the reflection name of a method or field. This annotation overrides anything previously set in other
 * annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Name {

    String value();
}
