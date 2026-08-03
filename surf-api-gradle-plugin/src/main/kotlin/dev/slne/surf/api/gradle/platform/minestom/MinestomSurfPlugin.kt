package dev.slne.surf.api.gradle.platform.minestom

import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin

internal class MinestomSurfPlugin : AbstractCoreSurfPlugin<MinestomSurfExtension>(
    platformName = "minestom",
    platform = SurfApiPlatform.MINESTOM,
) {
    override val extensionClass = MinestomSurfExtension::class.java
}