package dev.slne.surf.surfapi.core.api.packet.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jetbrains.annotations.ApiStatus;

/**
 * This annotation indicates that the specified method is useless and don't have any effect on the
 * entity. It is only there for the sake of completeness
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Documented
@ApiStatus.Obsolete
public @interface Useless {

}
