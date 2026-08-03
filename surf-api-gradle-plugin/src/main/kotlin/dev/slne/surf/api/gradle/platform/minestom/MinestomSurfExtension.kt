package dev.slne.surf.api.gradle.platform.minestom

import dev.slne.surf.api.gradle.platform.core.CoreSurfExtension
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class MinestomSurfExtension @Inject constructor(
    objects: ObjectFactory,
) : CoreSurfExtension(objects)