package dev.slne.surf.surfapi.core.api.packet.entity.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * If this annotation is present, the entity must be respawned after invoking the method to see the changes.
 */
@Retention(RetentionPolicy.SOURCE)
@java.lang.annotation.Target(ElementType.METHOD)
@Documented
public @interface NeedsRespawn {
}
