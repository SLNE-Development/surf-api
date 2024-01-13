package dev.slne.surf.surfapi.core.api.reflection.annontation;

import java.lang.annotation.*;

/**
 * Marks a method in a {@link SurfProxy} as a static method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Static {

    String name() default "";
}
