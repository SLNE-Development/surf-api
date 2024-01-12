package dev.slne.surf.surfapi.core.api.reflection.annontation;

public @interface Field {

    String name() default "";

    Type type();

    enum Type {
        SETTER,
        GETTER
    }
}
