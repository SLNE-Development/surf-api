package dev.slne.surf.surfapi.core.api.packet.entity.annotation;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.*;

/**
 * This annotation indicates that the specified method is useless and don't have any effect on the entity.
 * It is only there for the sake of completeness
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Documented
@ApiStatus.Obsolete
public @interface Useless {
}
