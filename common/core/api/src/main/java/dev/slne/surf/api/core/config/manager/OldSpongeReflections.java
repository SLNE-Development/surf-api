package dev.slne.surf.api.core.config.manager;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SuppressWarnings("unchecked")
@NullMarked
final class OldSpongeReflections {
    static final Class<? extends Annotation> OLD_CONFIG_SERIALIZABLE_ANNOTATION;
    static final Class<? extends Annotation> OLD_COMMENT_ANNOTATION;
    static final Class<? extends Annotation> OLD_MATCHES_ANNOTATION;
    static final Class<? extends Annotation> OLD_REQUIRED_ANNOTATION;
    static final Class<? extends Annotation> OLD_SETTING_ANNOTATION;

    private static final MethodHandle OLD_COMMENT_OVERRIDE;
    private static final MethodHandle OLD_COMMENT_VALUE;

    private static final MethodHandle OLD_MATCHES_VALUE;
    private static final MethodHandle OLD_MATCHES_FLAGS;
    private static final MethodHandle OLD_MATCHES_FAILURE_MESSAGE;

    private static final MethodHandle OLD_SETTING_VALUE;
    private static final MethodHandle OLD_SETTING_NODE_FROM_PARENT;

    static boolean isCommentOverride(final Annotation annotation) throws Throwable {
        return (boolean) OLD_COMMENT_OVERRIDE.invoke(annotation);
    }

    static String getCommentValue(final Annotation annotation) throws Throwable {
        return (String) OLD_COMMENT_VALUE.invoke(annotation);
    }

    static String getMatchesValue(final Annotation annotation) throws Throwable {
        return (String) OLD_MATCHES_VALUE.invoke(annotation);
    }

    static int getMatchesFlags(final Annotation annotation) throws Throwable {
        return (int) OLD_MATCHES_FLAGS.invoke(annotation);
    }

    static String getMatchesFailureMessage(final Annotation annotation) throws Throwable {
        return (String) OLD_MATCHES_FAILURE_MESSAGE.invoke(annotation);
    }

    static String getSettingValue(final Annotation annotation) throws Throwable {
        return (String) OLD_SETTING_VALUE.invoke(annotation);
    }

    static boolean isSettingNodeFromParent(final Annotation annotation) throws Throwable {
        return (boolean) OLD_SETTING_NODE_FROM_PARENT.invoke(annotation);
    }

    static {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();

            OLD_CONFIG_SERIALIZABLE_ANNOTATION = (Class<? extends Annotation>) Class.forName("org.spongepowered".concat(".configurate.objectmapping.ConfigSerializable"));
            OLD_COMMENT_ANNOTATION = (Class<? extends Annotation>) Class.forName("org.spongepowered".concat(".configurate.objectmapping.meta.Comment"));
            OLD_MATCHES_ANNOTATION = (Class<? extends Annotation>) Class.forName("org.spongepowered".concat(".configurate.objectmapping.meta.Matches"));
            OLD_REQUIRED_ANNOTATION = (Class<? extends Annotation>) Class.forName("org.spongepowered".concat(".configurate.objectmapping.meta.Required"));
            OLD_SETTING_ANNOTATION = (Class<? extends Annotation>) Class.forName("org.spongepowered".concat(".configurate.objectmapping.meta.Setting"));

            OLD_COMMENT_OVERRIDE = lookup.findVirtual(OLD_COMMENT_ANNOTATION, "override", MethodType.methodType(boolean.class));
            OLD_COMMENT_VALUE = lookup.findVirtual(OLD_COMMENT_ANNOTATION, "value", MethodType.methodType(String.class));

            OLD_MATCHES_VALUE = lookup.findVirtual(OLD_MATCHES_ANNOTATION, "value", MethodType.methodType(String.class));
            OLD_MATCHES_FLAGS = lookup.findVirtual(OLD_MATCHES_ANNOTATION, "flags", MethodType.methodType(int.class));
            OLD_MATCHES_FAILURE_MESSAGE = lookup.findVirtual(OLD_MATCHES_ANNOTATION, "failureMessage", MethodType.methodType(String.class));

            OLD_SETTING_VALUE = lookup.findVirtual(OLD_SETTING_ANNOTATION, "value", MethodType.methodType(String.class));
            OLD_SETTING_NODE_FROM_PARENT = lookup.findVirtual(OLD_SETTING_ANNOTATION, "nodeFromParent", MethodType.methodType(boolean.class));

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
