package dev.slne.surf.surfapi.core.api.reflection.annontation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SurfProxy {

    Class<?> value() default void.class;

    String qualifiedName() default "";
}
